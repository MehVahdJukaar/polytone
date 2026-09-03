package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.shaders.sodium.SodiumShadowRenderer;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Replays the level's already-compiled chunk VBOs from the light's point of view; nothing is re-meshed
public class ShadowMapRenderer {

    private ShadowMapSettings settings = ShadowMapSettings.DEFAULT;

    private final Matrix4f lastRenderedShadowMatrix = new Matrix4f();
    private Vec3 lastRenderedCamPos = Vec3.ZERO;
    private ClientLevel lastRenderedLevel = null;
    private long lastRenderMs = 0L;
    private boolean hasRenderedMap = false;

    private boolean renderingShadowPass = false;

    private TextureTarget shadowTarget = null;
    private final Matrix4f lightView = new Matrix4f();
    private final Matrix4f lightProj = new Matrix4f();
    private final Matrix4f shadowMatrix = new Matrix4f();
    private final Vector3f towardLight = new Vector3f(0, 1, 0);
    private final Vector3f cameraFract = new Vector3f();
    private final List<SectionRenderDispatcher.RenderSection> casterSections = new ArrayList<>();

    private ShaderInstance opaqueDepthShader = null;
    private ShaderInstance cutoutDepthShader = null;
    private boolean depthShadersFailed = false;

    public void setSettings(ShadowMapSettings settings) {
        this.settings = settings;
        this.hasRenderedMap = false;
        this.lastRenderedLevel = null;
    }

    public int getShadowTextureId() {
        return shadowTarget == null ? 0 : shadowTarget.getDepthTextureId();
    }

    public Matrix4f getShadowMatrix() {
        return shadowMatrix;
    }

    public Vector3f getTowardLight() {
        return towardLight;
    }

    public Vector3f getCameraFract() {
        return cameraFract;
    }

    public void renderShadowPassIfNeeded(Camera cam, Matrix4f cameraFrustumMatrix, Matrix4f cameraProjectionMatrix) {
        if (renderingShadowPass) return;
        if (!Polytone.POST_SHADERS.anyActiveEffectUsesShadowMap()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !cam.isInitialized()) return;

        Vec3 camPos = cam.getPosition();
        // every frame, even when the map is reused: the resolve shader's world-grid snap tracks the live camera
        cameraFract.set((float) Mth.frac(camPos.x), (float) Mth.frac(camPos.y), (float) Mth.frac(camPos.z));

        long now = Util.getMillis();
        float updateInterval = settings.updateInterval();
        // the re-align below only holds while the camera is still well inside the box the map was
        // rendered around; a teleport or a dimension change would slide the whole map away
        float maxReuseDrift = settings.coverage() * 0.25f;
        boolean reusable = hasRenderedMap && level == lastRenderedLevel
                && camPos.distanceToSqr(lastRenderedCamPos) <= maxReuseDrift * maxReuseDrift;
        boolean due = !reusable || updateInterval <= 0f || (now - lastRenderMs) >= updateInterval * 50f;
        if (due) {
            float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);
            boolean completed = true;
            renderingShadowPass = true;
            try {
                renderShadowMap(mc, level, cam, camPos, partialTick, cameraFrustumMatrix, cameraProjectionMatrix);
            } catch (Exception e) {
                // renderShadowMap restores its own GL state in finally blocks, so a failed pass can't
                // propagate into renderLevel
                completed = false;
                Polytone.LOGGER.error("Polytone shadow-map render failed", e);
            } finally {
                renderingShadowPass = false;
            }
            if (completed) {
                lastRenderedShadowMatrix.set(shadowMatrix);
                lastRenderedCamPos = camPos;
                lastRenderedLevel = level;
                lastRenderMs = now;
                hasRenderedMap = true;
            } else {
                hasRenderedMap = false;
            }
        } else {
            // the projection is orthographic and the light basis is fixed between updates, so a plain
            // translate by the camera delta re-aligns the map we already have
            shadowMatrix.set(lastRenderedShadowMatrix).translate(
                    (float) (camPos.x - lastRenderedCamPos.x),
                    (float) (camPos.y - lastRenderedCamPos.y),
                    (float) (camPos.z - lastRenderedCamPos.z));
        }
    }

    private void renderShadowMap(Minecraft mc, ClientLevel level, Camera cam, Vec3 camPos, float partialTick,
                                 Matrix4f cameraFrustumMatrix, Matrix4f cameraProjectionMatrix) {
        ensureTarget();
        updateLightMatrices(level, camPos, partialTick);

        ShadowCasterVolume volume = new ShadowCasterVolume(lightView, settings.coverage(), settings.depthRange());
        boolean rendersEveryFrame = settings.updateInterval() <= 0f;
        if (rendersEveryFrame && cameraFrustumMatrix != null && cameraProjectionMatrix != null) {
            volume.buildCasterPlanes(cameraProjectionMatrix.mul(cameraFrustumMatrix, new Matrix4f()), towardLight);
        }

        collectCasterSections(mc, volume, camPos);

        RenderTarget mainTarget = mc.getMainRenderTarget();

        Matrix4f savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting savedSorting = RenderSystem.getVertexSorting();

        shadowTarget.bindWrite(true);
        RenderSystem.depthMask(true);
        GlStateManager._clearDepth(1.0);
        GlStateManager._clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        try {
            if (!casterSections.isEmpty()) {
                drawLayer(mc, RenderType.solid(), camPos, depthShader(mc, false));
                drawLayer(mc, RenderType.cutoutMipped(), camPos, depthShader(mc, true));
                drawLayer(mc, RenderType.cutout(), camPos, depthShader(mc, true));
            } else if (CompatHandler.SODIUM) {
                SodiumShadowRenderer.replayTerrain(mc, cam, camPos, lightView, lightProj, volume);
            }

            if (settings.renderEntities() || settings.renderBlockEntities()) {
                drawEntities(mc, level, camPos, volume);
            }
        } finally {
            mainTarget.bindWrite(true);
            RenderSystem.setProjectionMatrix(savedProjection, savedSorting);
            RenderSystem.applyModelViewMatrix();
        }
    }

    private void updateLightMatrices(ClientLevel level, Vec3 camPos, float partialTick) {
        float coverage = settings.coverage();
        float depthRange = settings.depthRange();

        updateTowardLight(level, partialTick);
        // any up vector does, as long as it isn't parallel to the light
        boolean lightNearlyVertical = Math.abs(towardLight.y) > 0.99f;
        lightView.setLookAlong(-towardLight.x, -towardLight.y, -towardLight.z,
                0f, lightNearlyVertical ? 0f : 1f, lightNearlyVertical ? 1f : 0f);
        lightProj.setOrtho(-coverage, coverage, -coverage, coverage, -depthRange, depthRange);

        // texel snap, anchored to the camera's chunk corner; known accepted artifact, see research/POST_SHADOW_NOTES.md
        double anchorX = Mth.positiveModulo(camPos.x, 16.0);
        double anchorY = Mth.positiveModulo(camPos.y, 16.0);
        double anchorZ = Mth.positiveModulo(camPos.z, 16.0);
        double lightSpaceX = lightView.m00() * anchorX + lightView.m10() * anchorY + lightView.m20() * anchorZ;
        double lightSpaceY = lightView.m01() * anchorX + lightView.m11() * anchorY + lightView.m21() * anchorZ;
        double texelSize = 2.0 * coverage / settings.resolution();
        lightProj.m30(lightProj.m30() + (float) (offsetToTexelGrid(lightSpaceX, texelSize) / coverage));
        lightProj.m31(lightProj.m31() + (float) (offsetToTexelGrid(lightSpaceY, texelSize) / coverage));

        shadowMatrix.set(lightProj).mul(lightView);
    }

    private static double offsetToTexelGrid(double lightSpaceCoord, double texelSize) {
        return lightSpaceCoord - Math.round(lightSpaceCoord / texelSize) * texelSize;
    }

    // matches vanilla's sun placement in renderSky, and flips to the moon once it drops below the horizon
    private void updateTowardLight(ClientLevel level, float partialTick) {
        float sunAngle = level.getSunAngle(partialTick);
        towardLight.set(-Mth.sin(sunAngle), Mth.cos(sunAngle), 0f);
        if (towardLight.y < 0f) towardLight.negate();
    }

    private void drawEntities(Minecraft mc, ClientLevel level, Vec3 camPos, ShadowCasterVolume volume) {
        var entityDispatcher = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        try {
            modelViewStack.mul(lightView);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(lightProj, VertexSorting.ORTHOGRAPHIC_Z);

            PoseStack poseStack = new PoseStack();
            if (settings.renderEntities()) {
                TickRateManager tickRate = level.tickRateManager();
                float runningPartialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);
                float frozenPartialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);

                for (Entity entity : level.entitiesForRendering()) {
                    if (entity.isSpectator()) continue;

                    AABB box = entity.getBoundingBox();
                    float radius = (float) Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize()));
                    if (!canCastIntoView(volume, camPos, (box.minX + box.maxX) * 0.5,
                            (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5, radius)) continue;

                    float partialTick = tickRate.isEntityFrozen(entity) ? frozenPartialTick : runningPartialTick;
                    Vec3 pos = entity.getPosition(partialTick);
                    try {
                        // fullbright: only depth matters, skip the per-entity light lookup
                        entityDispatcher.render(entity, pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z,
                                entity.getViewYRot(partialTick), partialTick, poseStack, bufferSource,
                                LightTexture.FULL_BRIGHT);
                    } catch (Exception e) {
                        // one broken entity renderer must not kill the frame
                    }
                }
            }

            if (settings.renderBlockEntities()) {
                drawBlockEntities(mc, level, camPos, bufferSource, poseStack, volume);
            }

            try {
                bufferSource.endBatch();
            } catch (Exception e) {
                Polytone.LOGGER.error("Error flushing polytone shadow entity batch", e);
            }
        } finally {
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    private void drawBlockEntities(Minecraft mc, ClientLevel level, Vec3 camPos,
                                   MultiBufferSource bufferSource, PoseStack poseStack,
                                   ShadowCasterVolume volume) {
        BlockEntityRenderDispatcher blockEntityDispatcher = mc.getBlockEntityRenderDispatcher();
        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);
        float radius = 1.5f; // most fit in a block, slack for taller ones like beds and chests

        if (!casterSections.isEmpty()) {
            for (SectionRenderDispatcher.RenderSection section : casterSections) {
                for (BlockEntity blockEntity : section.getCompiled().getRenderableBlockEntities()) {
                    renderBlockEntity(blockEntityDispatcher, blockEntity, camPos, bufferSource, poseStack,
                            volume, radius, partialTick);
                }
            }
            return;
        }

        int camChunkX = Mth.floor(camPos.x) >> 4;
        int camChunkZ = Mth.floor(camPos.z) >> 4;
        int chunkRadius = Mth.ceil(settings.coverage() / 16f) + 1;

        for (int cx = camChunkX - chunkRadius; cx <= camChunkX + chunkRadius; cx++) {
            for (int cz = camChunkZ - chunkRadius; cz <= camChunkZ + chunkRadius; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    renderBlockEntity(blockEntityDispatcher, entry.getValue(), camPos, bufferSource, poseStack,
                            volume, radius, partialTick);
                }
            }
        }
    }

    private static void renderBlockEntity(BlockEntityRenderDispatcher blockEntityDispatcher, BlockEntity blockEntity,
                                          Vec3 camPos, MultiBufferSource bufferSource, PoseStack poseStack,
                                          ShadowCasterVolume volume, float radius, float partialTick) {
        BlockPos pos = blockEntity.getBlockPos();
        if (!canCastIntoView(volume, camPos, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, radius)) return;

        poseStack.pushPose();
        poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
        try {
            blockEntityDispatcher.render(blockEntity, partialTick, poseStack, bufferSource);
        } catch (Exception ignored) {
        }
        poseStack.popPose();
    }

    private void collectCasterSections(Minecraft mc, ShadowCasterVolume volume, Vec3 camPos) {
        casterSections.clear();
        ViewArea viewArea = mc.levelRenderer.viewArea;
        if (viewArea == null) return;

        for (SectionRenderDispatcher.RenderSection section : viewArea.sections) {
            SectionRenderDispatcher.CompiledSection compiled = section.getCompiled();
            if (compiled == SectionRenderDispatcher.CompiledSection.UNCOMPILED
                    || compiled.hasNoRenderableLayers()) continue;

            BlockPos origin = section.getOrigin();
            if (canCastIntoView(volume, camPos, origin.getX() + 8, origin.getY() + 8, origin.getZ() + 8, 8f)) {
                casterSections.add(section);
            }
        }
    }

    private static boolean canCastIntoView(ShadowCasterVolume volume, Vec3 camPos,
                                         double x, double y, double z, float halfExtent) {
        return volume.intersects((float) (x - camPos.x), (float) (y - camPos.y), (float) (z - camPos.z),
                halfExtent, halfExtent, halfExtent);
    }

    private void drawLayer(Minecraft mc, RenderType renderType, Vec3 camPos, ShaderInstance depthShader) {
        renderType.setupRenderState();
        ShaderInstance shader = depthShader != null ? depthShader : RenderSystem.getShader();
        if (shader == null) {
            renderType.clearRenderState();
            return;
        }
        try {
            if (depthShader != null) {
                depthShader.setSampler("Sampler0", RenderSystem.getShaderTexture(0));
            }
            shader.setDefaultUniforms(VertexFormat.Mode.QUADS, lightView, lightProj, mc.getWindow());
            shader.apply();
            Uniform chunkOffset = shader.CHUNK_OFFSET;

            for (SectionRenderDispatcher.RenderSection section : casterSections) {
                if (section.getCompiled().isEmpty(renderType)) continue;
                if (chunkOffset != null) {
                    BlockPos origin = section.getOrigin();
                    chunkOffset.set(
                            (float) (origin.getX() - camPos.x),
                            (float) (origin.getY() - camPos.y),
                            (float) (origin.getZ() - camPos.z));
                    chunkOffset.upload();
                }
                VertexBuffer buffer = section.getBuffer(renderType);
                buffer.bind();
                buffer.draw();
            }

            if (chunkOffset != null) chunkOffset.set(0f, 0f, 0f);
        } finally {
            shader.clear();
            VertexBuffer.unbind();
            renderType.clearRenderState();
        }
    }

    private ShaderInstance depthShader(Minecraft mc, boolean cutout) {
        if (depthShadersFailed) return null;
        ShaderInstance cached = cutout ? cutoutDepthShader : opaqueDepthShader;
        if (cached != null) return cached;
        try {
            ShaderInstance loaded = new ShaderInstance(mc.getResourceManager(),
                    cutout ? "polytone_shadow_terrain_cutout" : "polytone_shadow_terrain",
                    DefaultVertexFormat.BLOCK);
            if (cutout) cutoutDepthShader = loaded;
            else opaqueDepthShader = loaded;
            return loaded;
        } catch (Exception e) {
            depthShadersFailed = true;
            Polytone.LOGGER.error("Failed to load the polytone shadow depth shaders; " +
                    "falling back to the vanilla terrain programs for the shadow pass", e);
            return null;
        }
    }

    private void ensureTarget() {
        int resolution = settings.resolution();
        if (shadowTarget == null || shadowTarget.width != resolution) {
            if (shadowTarget != null) shadowTarget.destroyBuffers();
            shadowTarget = new TextureTarget(resolution, resolution, true, Minecraft.ON_OSX);

            int previous = GlStateManager.getBoundFramebuffer();
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, shadowTarget.frameBufferId);
            GL11.glDrawBuffer(GL11.GL_NONE);
            GL11.glReadBuffer(GL11.GL_NONE);
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, previous);
        }
    }

    public void close() {
        if (shadowTarget != null) {
            shadowTarget.destroyBuffers();
            shadowTarget = null;
        }
        if (opaqueDepthShader != null) {
            opaqueDepthShader.close();
            opaqueDepthShader = null;
        }
        if (cutoutDepthShader != null) {
            cutoutDepthShader.close();
            cutoutDepthShader = null;
        }
        depthShadersFailed = false;
        hasRenderedMap = false;
        lastRenderedLevel = null;
        casterSections.clear();
    }
}
