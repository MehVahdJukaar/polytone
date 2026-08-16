package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.shaders.sodium.SodiumShadowRenderer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.mixins.accessor.LevelRendererShadowAccessor;
import net.minecraft.util.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Optional;

public class ShadowMapRenderer {

    private static final ChunkSectionLayer[] SHADOW_LAYERS = {ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT};

    private ShadowMapSettings settings = ShadowMapSettings.DEFAULT;

    private final Matrix4f lastRenderedShadowMatrix = new Matrix4f();
    private Vec3 lastRenderedCamPos = Vec3.ZERO;
    private ClientLevel lastRenderedLevel = null;
    private long lastRenderMs = 0L;
    private boolean hasRenderedMap = false;

    private boolean renderingShadowPass = false;

    private GpuTexture depthTexture = null;
    private GpuTextureView depthTextureView = null;
    private GpuTexture colorTexture = null; // never sampled, render passes just need a color attachment
    private GpuTextureView colorTextureView = null;

    private GpuBuffer lightProjectionBuffer = null;

    private PolyShadowUniforms uniforms = null;

    private final Matrix4f shadowMatrix = new Matrix4f();
    private final Vector3f towardLight = new Vector3f(0, 1, 0);
    private final Vector3f cameraFract = new Vector3f();
    private final List<SectionRenderDispatcher.RenderSection> casterSections = new ArrayList<>();
    private final List<BlockEntity> casterBlockEntities = new ArrayList<>();

    public void setSettings(ShadowMapSettings settings) {
        this.settings = settings;
        this.hasRenderedMap = false;
        this.lastRenderedLevel = null;
    }

    public GpuTextureView getShadowTexture() {
        return depthTextureView;
    }

    @Nullable
    public GpuBufferSlice getUniformsSlice() {
        return uniforms == null ? null : uniforms.getSlice();
    }

    public void renderShadowPassIfNeeded(GpuBufferSlice shaderFog, Camera cam,
                                         Matrix4fc cameraFrustumMatrix, Matrix4f cameraProjectionMatrix) {
        if (renderingShadowPass) return;
        if (!Polytone.POST_CHAINS.anyActiveChainWantsShadowMap()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !cam.isInitialized()) return;

        Vec3 camPos = cam.position();
        cameraFract.set((float) Mth.frac(camPos.x), (float) Mth.frac(camPos.y), (float) Mth.frac(camPos.z));

        long now = Util.getMillis();
        float updateInterval = settings.updateInterval();
        float maxReuseDrift = settings.coverage() * 0.25f;
        boolean reusable = hasRenderedMap && level == lastRenderedLevel
                && camPos.distanceToSqr(lastRenderedCamPos) <= maxReuseDrift * maxReuseDrift;
        boolean due = !reusable || updateInterval <= 0f || (now - lastRenderMs) >= updateInterval * 50f;
        if (due) {
            float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            boolean completed = true;
            renderingShadowPass = true;
            try {
                renderShadowMap(mc, level, cam, camPos, partialTick, shaderFog, cameraFrustumMatrix, cameraProjectionMatrix);
            } catch (Exception e) {
                completed = false;
                Polytone.LOGGER.error("Polytone shadow-map render failed", e);
            } finally {
                renderingShadowPass = false;
            }
            // a half-drawn map must not be reused for the whole update interval
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
            shadowMatrix.set(lastRenderedShadowMatrix).translate(
                    (float) (camPos.x - lastRenderedCamPos.x),
                    (float) (camPos.y - lastRenderedCamPos.y),
                    (float) (camPos.z - lastRenderedCamPos.z));
        }
        if (uniforms == null) uniforms = new PolyShadowUniforms();
        uniforms.update(shadowMatrix, towardLight, cameraFract);
    }

    private void renderShadowMap(Minecraft mc, ClientLevel level, Camera cam, Vec3 camPos, float partialTick,
                                 GpuBufferSlice shaderFog, Matrix4fc cameraFrustumMatrix, Matrix4f cameraProjectionMatrix) {
        ensureGpuResources();

        float coverage = settings.coverage();
        float depthRange = settings.depthRange();

        towardLight.set(directionTowardLight(cam, partialTick));
        Vector3f up = Math.abs(towardLight.y) > 0.99f ? new Vector3f(0, 0, 1) : new Vector3f(0, 1, 0);

        Matrix4f lightView = new Matrix4f().lookAlong(
                -towardLight.x, -towardLight.y, -towardLight.z, up.x, up.y, up.z);
        Matrix4f lightProj = new Matrix4f().ortho(
                -coverage, coverage, -coverage, coverage, -depthRange, depthRange);

        // texel snap, anchored to the camera's chunk corner; known accepted artifact, see research/POST_SHADOW_NOTES.md
        double anchorX = camPos.x - Math.floor(camPos.x / 16.0) * 16.0;
        double anchorY = camPos.y - Math.floor(camPos.y / 16.0) * 16.0;
        double anchorZ = camPos.z - Math.floor(camPos.z / 16.0) * 16.0;
        double lightSpaceX = lightView.m00() * anchorX + lightView.m10() * anchorY + lightView.m20() * anchorZ;
        double lightSpaceY = lightView.m01() * anchorX + lightView.m11() * anchorY + lightView.m21() * anchorZ;
        double texelSize = 2.0 * coverage / settings.resolution();
        lightProj.m30(lightProj.m30() + (float) ((lightSpaceX - Math.round(lightSpaceX / texelSize) * texelSize) / coverage));
        lightProj.m31(lightProj.m31() + (float) ((lightSpaceY - Math.round(lightSpaceY / texelSize) * texelSize) / coverage));

        shadowMatrix.set(lightProj).mul(lightView);

        ShadowCasterVolume volume = new ShadowCasterVolume(lightView, coverage, depthRange);
        boolean rendersEveryFrame = settings.updateInterval() <= 0f;
        if (rendersEveryFrame && cameraFrustumMatrix != null && cameraProjectionMatrix != null) {
            volume.buildCasterPlanes(cameraProjectionMatrix.mul(cameraFrustumMatrix, new Matrix4f()), towardLight);
        }

        collectCasterSections(mc, volume, camPos);

        GpuDevice device = RenderSystem.getDevice();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer bytes = Std140Builder.onStack(stack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE)
                    .putMat4f(lightProj).get();
            device.createCommandEncoder().writeToBuffer(lightProjectionBuffer.slice(), bytes);
        }

        // always clear, even with nothing to draw: a stale map has last frame's projection
        device.createCommandEncoder().clearColorAndDepthTextures(colorTexture, new Vector4f(0, 0, 0, 0), depthTexture, 1.0);

        RenderSystem.setShaderFog(shaderFog);

        if (CompatHandler.SODIUM) {
            SodiumShadowRenderer.replayTerrain(mc, cam, camPos, lightView, lightProj,
                    volume, colorTextureView, depthTextureView, casterBlockEntities);
        } else {
            drawVanillaTerrain(mc, camPos, lightView);
        }
        if (settings.renderEntities() || settings.renderBlockEntities()) {
            drawEntitiesAndBlockEntities(mc, level, camPos, lightView, volume);
        } else {
            casterBlockEntities.clear();
        }
    }

    private void drawVanillaTerrain(Minecraft mc, Vec3 camPos, Matrix4f lightView) {
        if (casterSections.isEmpty()) return;

        SectionRenderDispatcher dispatcher =
                ((LevelRendererShadowAccessor) mc.levelRenderer).polytone$getSectionRenderDispatcher();
        if (dispatcher == null) return;

        GpuTextureView atlasView = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        int atlasWidth = atlasView.getWidth(0);
        int atlasHeight = atlasView.getHeight(0);

        EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>> drawsPerLayer =
                new EnumMap<>(ChunkSectionLayer.class);
        for (ChunkSectionLayer layer : SHADOW_LAYERS) {
            drawsPerLayer.put(layer, new Int2ObjectOpenHashMap<>());
        }
        List<DynamicUniforms.ChunkSectionInfo> sectionInfos = new ArrayList<>();
        int maxIndices = 0;
        long now = Util.getMillis();

        // read only under the lock: an upload here would move allocations under the main pass's frozen draws
        dispatcher.lock();
        try {
            for (SectionRenderDispatcher.RenderSection section : casterSections) {
                SectionMesh mesh = section.getSectionMesh();
                BlockPos origin = section.getRenderOrigin();
                int infoIndex = -1;
                for (ChunkSectionLayer layer : SHADOW_LAYERS) {
                    SectionMesh.SectionDraw draw = mesh.getSectionDraw(layer);
                    SectionRenderDispatcher.RenderSectionBufferSlice slice =
                            dispatcher.getRenderSectionSlice(mesh, layer);
                    if (draw == null || slice == null) continue;
                    if (draw.hasCustomIndexBuffer() && slice.indexBuffer() == null) continue;
                    if (infoIndex == -1) {
                        infoIndex = sectionInfos.size();
                        sectionInfos.add(new DynamicUniforms.ChunkSectionInfo(new Matrix4f(lightView),
                                origin.getX(), origin.getY(), origin.getZ(),
                                section.getVisibility(now), atlasWidth, atlasHeight));
                    }
                    VertexFormat vertexFormat = layer.pipeline().getVertexFormatBinding(0);
                    GpuBuffer vertexBuffer = slice.vertexBuffer();
                    int bufferGroup = 31 * 173 + vertexBuffer.hashCode();

                    int firstIndex = 0;
                    GpuBuffer indexBuffer;
                    IndexType indexType;
                    if (!draw.hasCustomIndexBuffer()) {
                        maxIndices = Math.max(maxIndices, draw.indexCount());
                        indexBuffer = null;
                        indexType = null;
                    } else {
                        indexBuffer = slice.indexBuffer();
                        indexType = draw.indexType();
                        bufferGroup = 31 * bufferGroup + indexBuffer.hashCode();
                        bufferGroup = 31 * bufferGroup + indexType.hashCode();
                        firstIndex = (int) (slice.indexBufferOffset() / indexType.bytes);
                    }
                    int baseVertex = (int) (slice.vertexBufferOffset() / vertexFormat.getVertexSize());
                    int uniformIndex = infoIndex;
                    drawsPerLayer.get(layer).computeIfAbsent(bufferGroup, k -> new ArrayList<>())
                            .add(new RenderPass.Draw<>(0, vertexBuffer, indexBuffer, indexType,
                                    firstIndex, draw.indexCount(), baseVertex,
                                    (slices, uploader) -> uploader.upload("ChunkSection", slices[uniformIndex])));
                }
            }
        } finally {
            dispatcher.unlock();
        }
        if (sectionInfos.isEmpty()) return;

        GpuBufferSlice[] slices = RenderSystem.getDynamicUniforms()
                .writeChunkSections(sectionInfos.toArray(new DynamicUniforms.ChunkSectionInfo[0]));

        RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer sharedIndexBuffer = maxIndices == 0 ? null : sequential.getBuffer(maxIndices);
        IndexType sharedIndexType = maxIndices == 0 ? null : sequential.type();

        GpuSampler atlasSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, true);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Polytone shadow map terrain", colorTextureView, Optional.empty(),
                depthTextureView, OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("Projection", lightProjectionBuffer.slice()); // after the defaults, last bind wins
            pass.bindTexture("Sampler2", mc.gameRenderer.lightmap(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            for (ChunkSectionLayer layer : SHADOW_LAYERS) {
                pass.setPipeline(layer.pipeline());
                pass.bindTexture("Sampler0", atlasView, atlasSampler);
                for (var draws : drawsPerLayer.get(layer).values()) {
                    if (draws.isEmpty()) continue;
                    pass.drawMultipleIndexed(draws, sharedIndexBuffer, sharedIndexType, List.of("ChunkSection"), slices);
                }
            }
        }
    }

    private void drawEntitiesAndBlockEntities(Minecraft mc, ClientLevel level, Vec3 camPos, Matrix4f lightView,
                                              ShadowCasterVolume volume) {
        EntityRenderDispatcher entityDispatcher = mc.getEntityRenderDispatcher();
        BlockEntityRenderDispatcher blockEntityDispatcher = mc.getBlockEntityRenderDispatcher();
        FeatureRenderDispatcher featureDispatcher = mc.gameRenderer.featureRenderDispatcher();
        CameraRenderState camState = mc.levelRenderer.levelRenderState.cameraRenderState;
        SubmitNodeStorage submitNodes = new SubmitNodeStorage();

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(lightProjectionBuffer.slice(), ProjectionType.ORTHOGRAPHIC);
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.set(lightView);
        RenderSystem.outputColorTextureOverride = colorTextureView;
        RenderSystem.outputDepthTextureOverride = depthTextureView;
        mc.gameRenderer.lighting().setupFor(Lighting.Entry.LEVEL);
        try {
            PoseStack poseStack = new PoseStack();

            if (settings.renderEntities()) {
                for (Entity entity : level.entitiesForRendering()) {
                    if (entity.isSpectator()) continue;

                    AABB box = entity.getBoundingBox();
                    float radius = (float) Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize()));
                    Vec3 center = box.getCenter();
                    if (!canCastIntoView(volume, camPos, center.x, center.y, center.z, radius)) continue;

                    float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(
                            !level.tickRateManager().isEntityFrozen(entity));
                    try {
                        EntityRenderState state = entityDispatcher.extractEntity(entity, partialTick);
                        entityDispatcher.submit(state, camState, state.x - camPos.x, state.y - camPos.y,
                                state.z - camPos.z, poseStack, submitNodes);
                    } catch (Exception e) {
                        // one broken entity renderer must not kill the frame
                    }
                }
            }

            if (settings.renderBlockEntities()) {
                submitBlockEntities(mc, blockEntityDispatcher, submitNodes, camState, camPos, poseStack);
            }

            try {
                featureDispatcher.renderAllFeatures(submitNodes);
            } catch (Exception e) {
                Polytone.LOGGER.error("Error rendering polytone shadow features", e);
            }
        } finally {
            casterBlockEntities.clear();
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            modelViewStack.popMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
    }

    private void submitBlockEntities(Minecraft mc, BlockEntityRenderDispatcher blockEntityDispatcher,
                                     SubmitNodeStorage submitNodes, CameraRenderState camState,
                                     Vec3 camPos, PoseStack poseStack) {
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (BlockEntity blockEntity : casterBlockEntities) {
            BlockPos pos = blockEntity.getBlockPos();
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
            try {
                var state = blockEntityDispatcher.tryExtractRenderState(blockEntity, partialTick, null, false);
                if (state != null) {
                    blockEntityDispatcher.submit(state, poseStack, submitNodes, camState);
                }
            } catch (Exception e) {
            }
            poseStack.popPose();
        }
    }

    private void collectCasterSections(Minecraft mc, ShadowCasterVolume volume, Vec3 camPos) {
        casterSections.clear();
        casterBlockEntities.clear();
        ViewArea viewArea = ((LevelRendererShadowAccessor) mc.levelRenderer).polytone$getViewArea();
        if (viewArea == null) return;

        for (SectionRenderDispatcher.RenderSection section : viewArea.sections) {
            if (!section.getSectionMesh().hasRenderableLayers()) continue;

            BlockPos origin = section.getRenderOrigin();
            if (canCastIntoView(volume, camPos, origin.getX() + 8, origin.getY() + 8, origin.getZ() + 8, 8f)) {
                casterSections.add(section);
                casterBlockEntities.addAll(section.getSectionMesh().getRenderableBlockEntities());
            }
        }
    }

    private static boolean canCastIntoView(ShadowCasterVolume volume, Vec3 camPos,
                                           double x, double y, double z, float halfExtent) {
        return volume.intersects((float) (x - camPos.x), (float) (y - camPos.y), (float) (z - camPos.z),
                halfExtent, halfExtent, halfExtent);
    }

    private static Vector3f directionTowardLight(Camera cam, float partialTick) {
        float angle = cam.attributeProbe().getValue(EnvironmentAttributes.SUN_ANGLE, partialTick) * Mth.DEG_TO_RAD;
        Vector3f sunDir = new Vector3f(-Mth.sin(angle), Mth.cos(angle), 0f);
        if (sunDir.y < 0f) sunDir.negate();
        return sunDir;
    }

    private void ensureGpuResources() {
        GpuDevice device = RenderSystem.getDevice();
        if (lightProjectionBuffer == null) {
            lightProjectionBuffer = device.createBuffer(() -> "Polytone shadow projection UBO",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
        }
        int resolution = settings.resolution();
        if (depthTexture == null || depthTexture.getWidth(0) != resolution) {
            closeTextures();
            depthTexture = device.createTexture(() -> "Polytone shadow map depth",
                    GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING
                            | GpuTexture.USAGE_COPY_DST,
                    GpuFormat.D32_FLOAT, resolution, resolution, 1, 1);
            depthTextureView = device.createTextureView(depthTexture);
            colorTexture = device.createTexture(() -> "Polytone shadow map color",
                    GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST,
                    GpuFormat.RGBA8_UNORM, resolution, resolution, 1, 1);
            colorTextureView = device.createTextureView(colorTexture);
        }
    }

    private void closeTextures() {
        if (depthTexture != null) {
            depthTextureView.close();
            depthTexture.close();
            colorTextureView.close();
            colorTexture.close();
            depthTexture = null;
            depthTextureView = null;
            colorTexture = null;
            colorTextureView = null;
        }
    }

    public void close() {
        closeTextures();
        if (lightProjectionBuffer != null) {
            lightProjectionBuffer.close();
            lightProjectionBuffer = null;
        }
        if (uniforms != null) {
            uniforms.close();
            uniforms = null;
        }
        casterSections.clear();
        casterBlockEntities.clear();
        hasRenderedMap = false;
        lastRenderedLevel = null;
    }
}
