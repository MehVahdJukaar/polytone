package net.mehvahdjukaar.polytone.content.shaders;

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
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.shaders.sodium.SodiumShadowRenderer;
import net.mehvahdjukaar.polytone.mixins.accessor.LevelRendererShadowAccessor;
import net.minecraft.util.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.CameraRenderState;
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
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

// Generates a directional (sun/moon) shadow depth map by replaying the level's already-compiled chunk
// section meshes from the light's point of view - nothing is re-meshed, we just draw the section
// buffers with the light's ortho matrices. Exposed to post chains as the InShadow sampler and the
// PolyShadow UBO (see PostChainsManager). The rendering half of the shadow system; ShadowMapManager
// owns the reloadable settings and feeds them here via setSettings().
public class ShadowMapRenderer {

    // Only opaque geometry casts: a shadow map stores occluder depth (CUTOUT still alpha-discards
    // in its own pipeline). TRANSLUCENT and TRIPWIRE are intentionally skipped.
    private static final ChunkSectionLayer[] SHADOW_LAYERS = {ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT};

    // Active parameters, pushed in by ShadowMapManager on reload (see ShadowMapSettings).
    private ShadowMapSettings settings = ShadowMapSettings.DEFAULT;

    // Reuse state: when we skip a re-render, the last map is kept and only re-aligned for camera motion.
    private final Matrix4f renderedMatrix = new Matrix4f();  // shadowMatrix at the last real render
    private Vec3 renderedCamPos = Vec3.ZERO;                 // camera position at the last real render
    private long lastUpdateMs = 0L;
    private boolean hasRendered = false;

    private GpuTexture depthTexture = null;
    private GpuTextureView depthTextureView = null;
    private GpuTexture colorTexture = null;      // never sampled: passes need a color attachment
    private GpuTextureView colorTextureView = null;

    // Our own "Projection" UBO holding the light's ortho projection, bound per-pass (terrain) or
    // swapped onto the RenderSystem global (entity replay) - the camera projection is never touched.
    private GpuBuffer projectionBuffer = null;

    // Created on first use: constructing it allocates a GPU buffer, and this class is instantiated
    // during Polytone's static init, before the GPU device exists.
    private PolyShadowUniforms uniforms = null;

    private final Matrix4f shadowMatrix = new Matrix4f();  // light view-proj -> PolyShadowMat
    private final Vector3f lightDir = new Vector3f(0, 1, 0);  // toward the light -> PolyShadowLightDir
    private final Vector3f camFract = new Vector3f();  // fract(camPos) -> PolyShadowCamFract (grid anchor)
    private final List<SectionRenderDispatcher.RenderSection> shadowSections = new ArrayList<>();

    // Swap in freshly reloaded settings; force a fresh render (and lazy target rebuild) on the next frame.
    public void setSettings(ShadowMapSettings settings) {
        this.settings = settings;
        this.hasRendered = false;
    }

    public GpuTextureView getShadowTexture() {
        return depthTextureView;
    }

    // Null until the first shadow pass has run this session (bind sites skip a null slice).
    @Nullable
    public GpuBufferSlice getUniformsSlice() {
        return uniforms == null ? null : uniforms.getSlice();
    }

    // Called from LevelRenderer.renderLevel HEAD (before the frame graph is built and executed, so the
    // post chains that sample us this frame see this frame's map); no render pass is open here, which
    // the UBO writes require. Skips all work unless some active post chain declares use_shadow_map.
    public void renderShadowPassIfNeeded(GpuBufferSlice shaderFog) {
        if (!Polytone.POST_CHAINS.anyActiveEffectUsesShadowMap()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Camera cam = mc.gameRenderer.getMainCamera();
        if (level == null || !cam.isInitialized()) return;

        Vec3 camPos = cam.position();
        // fract(camPos) every frame: the resolve shader's world-grid snap must track the live camera
        // even on frames where we reuse the map.
        camFract.set((float) Mth.frac(camPos.x), (float) Mth.frac(camPos.y), (float) Mth.frac(camPos.z));

        long now = Util.getMillis();
        float updateInterval = settings.updateInterval();
        boolean due = !hasRendered || updateInterval <= 0f || (now - lastUpdateMs) >= updateInterval * 50f;
        if (due) {
            float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            try {
                render(mc, level, cam, camPos, partial, shaderFog);
            } catch (Exception e) {
                // render() restores its own global state in finally blocks; swallow here so a failed
                // shadow pass can never propagate into renderLevel or leave the frame half-rendered.
                Polytone.LOGGER.error("Polytone shadow-map render failed", e);
            }
            renderedMatrix.set(shadowMatrix);
            renderedCamPos = camPos;
            lastUpdateMs = now;
            hasRendered = true;
        } else {
            // Reuse the last depth map; only re-align it for how far the camera has moved since. The
            // projection is orthographic and the light basis is fixed between updates, so a plain
            // translate by the camera delta maps current camera-relative positions back into the map.
            shadowMatrix.set(renderedMatrix).translate(
                    (float) (camPos.x - renderedCamPos.x),
                    (float) (camPos.y - renderedCamPos.y),
                    (float) (camPos.z - renderedCamPos.z));
        }
        // Post passes bind the PolyShadow UBO, so it must track the (possibly re-aligned) matrix every frame.
        if (uniforms == null) uniforms = new PolyShadowUniforms();
        uniforms.update(shadowMatrix, lightDir, camFract);
    }

    private void render(Minecraft mc, ClientLevel level, Camera cam, Vec3 camPos, float partial, GpuBufferSlice shaderFog) {
        ensureTarget();

        float coverage = settings.coverage();
        float depthRange = settings.depthRange();
        int shadowRes = settings.resolution();

        // Light direction: unit vector pointing FROM the scene TOWARD the sun (or moon at night).
        lightDir.set(computeLightDir(cam, partial));
        Vector3f up = Math.abs(lightDir.y) > 0.99f ? new Vector3f(0, 0, 1) : new Vector3f(0, 1, 0);

        // Built in camera-relative space (camera at origin) to match the camera-relative section
        // origins the terrain shader consumes per section. A single ortho cascade centered on the camera.
        Matrix4f lightView = new Matrix4f().lookAlong(
                -lightDir.x, -lightDir.y, -lightDir.z, up.x, up.y, up.z);
        Matrix4f lightProj = new Matrix4f().ortho(
                -coverage, coverage, -coverage, coverage, -depthRange, depthRange);

        // Texel snap keeps world geometry on the same shadow texels frame to frame (stops edge shimmer).
        // Anchored to the camera's CHUNK corner, not the world origin: the snap offset's sensitivity to a
        // rotating sun scales with distance to the anchor, so a world-origin anchor shimmered badly far
        // from spawn while a chunk-local one (<=16 blocks) does not. Doubles keep the mod exact.
        double ax = camPos.x - Math.floor(camPos.x / 16.0) * 16.0;
        double ay = camPos.y - Math.floor(camPos.y / 16.0) * 16.0;
        double az = camPos.z - Math.floor(camPos.z / 16.0) * 16.0;
        double sx = lightView.m00() * ax + lightView.m10() * ay + lightView.m20() * az;
        double sy = lightView.m01() * ax + lightView.m11() * ay + lightView.m21() * az;
        double texel = 2.0 * coverage / shadowRes; // world-space size of one shadow texel
        lightProj.m30(lightProj.m30() + (float) ((sx - Math.round(sx / texel) * texel) / coverage));
        lightProj.m31(lightProj.m31() + (float) ((sy - Math.round(sy / texel) * texel) / coverage));

        shadowMatrix.set(lightProj).mul(lightView);

        collectShadowSections(mc, lightView, camPos);

        GpuDevice device = RenderSystem.getDevice();

        // Upload the light projection to our own "Projection" UBO (no render pass may be open here).
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer bb = Std140Builder.onStack(stack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE)
                    .putMat4f(lightProj).get();
            device.createCommandEncoder().writeToBuffer(projectionBuffer.slice(), bb);
        }

        // Always clear to far depth (1.0), even when there's nothing to draw - a stale map would
        // shadow the world with last frame's (differently-projected) depth.
        device.createCommandEncoder().clearColorAndDepthTextures(colorTexture, 0, depthTexture, 1.0);

        // Fog is fetched globally by the draws below; make sure this frame's slice is current
        // (the main pass sets the same one at its own start).
        RenderSystem.setShaderFog(shaderFog);

        if (CompatHandler.SODIUM) {
            // Under Sodium the vanilla ViewArea sections are uncompiled, so drawTerrain draws nothing;
            // replay Sodium's own terrain from the light POV into the shadow attachments instead.
            SodiumShadowRenderer.replayTerrain(mc, cam, camPos, lightView, lightProj,
                    coverage, depthRange, colorTexture, depthTexture);
        } else {
            drawTerrain(mc, camPos, lightView);
        }
        drawEntitiesAndBlockEntities(mc, level, camPos, lightView);
    }

    // Every compiled section mesh intersecting the light volume drawn with the light matrices - the
    // body of vanilla's prepareChunkRenders + renderGroup, restricted to the opaque layers, with the
    // light view standing in for the frustum matrix and our ortho UBO bound as "Projection".
    private void drawTerrain(Minecraft mc, Vec3 camPos, Matrix4f lightView) {
        if (shadowSections.isEmpty()) return;

        GpuTextureView atlasView = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        int atlasW = atlasView.getWidth(0);
        int atlasH = atlasView.getHeight(0);

        EnumMap<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> drawsPerLayer = new EnumMap<>(ChunkSectionLayer.class);
        for (ChunkSectionLayer layer : SHADOW_LAYERS) {
            drawsPerLayer.put(layer, new ArrayList<>());
        }
        List<DynamicUniforms.ChunkSectionInfo> infos = new ArrayList<>();
        int maxIndices = 0;
        long now = Util.getMillis();

        for (SectionRenderDispatcher.RenderSection section : shadowSections) {
            SectionMesh mesh = section.getSectionMesh();
            BlockPos origin = section.getRenderOrigin();
            int infoIndex = -1;
            for (ChunkSectionLayer layer : SHADOW_LAYERS) {
                SectionBuffers buffers = mesh.getBuffers(layer);
                if (buffers == null) continue;
                if (infoIndex == -1) {
                    infoIndex = infos.size();
                    infos.add(new DynamicUniforms.ChunkSectionInfo(new Matrix4f(lightView),
                            origin.getX(), origin.getY(), origin.getZ(),
                            section.getVisibility(now), atlasW, atlasH));
                }
                GpuBuffer indexBuffer;
                VertexFormat.IndexType indexType;
                if (buffers.getIndexBuffer() == null) {
                    maxIndices = Math.max(maxIndices, buffers.getIndexCount());
                    indexBuffer = null;
                    indexType = null;
                } else {
                    indexBuffer = buffers.getIndexBuffer();
                    indexType = buffers.getIndexType();
                }
                int idx = infoIndex;
                drawsPerLayer.get(layer).add(new RenderPass.Draw<>(0, buffers.getVertexBuffer(), indexBuffer,
                        indexType, 0, buffers.getIndexCount(),
                        (slices, uploader) -> uploader.upload("ChunkSection", slices[idx])));
            }
        }
        if (infos.isEmpty()) return;

        GpuBufferSlice[] slices = RenderSystem.getDynamicUniforms()
                .writeChunkSections(infos.toArray(new DynamicUniforms.ChunkSectionInfo[0]));

        RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer sharedIndexBuffer = maxIndices == 0 ? null : sequential.getBuffer(maxIndices);
        VertexFormat.IndexType sharedIndexType = maxIndices == 0 ? null : sequential.type();

        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, true);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Polytone shadow map terrain", colorTextureView, OptionalInt.empty(),
                depthTextureView, OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("Projection", projectionBuffer.slice()); // after the defaults: last bind wins
            pass.bindTexture("Sampler2", mc.gameRenderer.lightTexture().getTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            for (ChunkSectionLayer layer : SHADOW_LAYERS) {
                List<RenderPass.Draw<GpuBufferSlice[]>> draws = drawsPerLayer.get(layer);
                if (draws.isEmpty()) continue;
                pass.setPipeline(layer.pipeline());
                pass.bindTexture("Sampler0", atlasView, sampler);
                pass.drawMultipleIndexed(draws, sharedIndexBuffer, sharedIndexType, List.of("ChunkSection"), slices);
            }
        }
    }

    // Entities and block entities aren't in the section meshes - extract and submit them like the main
    // pass does, then flush the feature dispatcher with the light matrices swapped onto the RenderSystem
    // globals and the shadow target as output override. Light-volume culled; unlike vanilla we include
    // the camera entity so the player casts a shadow in first person.
    private void drawEntitiesAndBlockEntities(Minecraft mc, ClientLevel level, Vec3 camPos, Matrix4f lightView) {
        float coverage = settings.coverage();
        float depthRange = settings.depthRange();

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        BlockEntityRenderDispatcher beDispatcher = mc.getBlockEntityRenderDispatcher();
        FeatureRenderDispatcher featureDispatcher = mc.gameRenderer.getFeatureRenderDispatcher();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        CameraRenderState camState = mc.levelRenderer.levelRenderState.cameraRenderState;

        // Swap the light matrices + shadow target onto the RenderSystem globals the feature draws
        // read. Everything is restored in the finally no matter what throws, so a failure can never
        // leave later frames rendering from the sun's POV or into the shadow target.
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionBuffer.slice(), ProjectionType.ORTHOGRAPHIC);
        Matrix4fStack mvStack = RenderSystem.getModelViewStack();
        mvStack.pushMatrix();
        mvStack.set(lightView);
        RenderSystem.outputColorTextureOverride = colorTextureView;
        RenderSystem.outputDepthTextureOverride = depthTextureView;
        // Only depth matters, but entity shaders still evaluate diffuse light; use the level setup.
        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);
        try {
            PoseStack poseStack = new PoseStack();
            Vector3f center = new Vector3f();

            // Light-volume cull, same separating-axis idea as the sections but with a scalar radius.
            for (Entity entity : level.entitiesForRendering()) {
                if (entity.isSpectator()) continue;

                AABB bb = entity.getBoundingBox();
                float radius = (float) Math.max(bb.getXsize(), Math.max(bb.getYsize(), bb.getZsize()));
                Vec3 c = bb.getCenter();
                center.set((float) (c.x - camPos.x), (float) (c.y - camPos.y), (float) (c.z - camPos.z));
                if (!insideLightBox(lightView, center, coverage, depthRange, radius, radius, radius)) continue;

                float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(
                        !level.tickRateManager().isEntityFrozen(entity));
                try {
                    EntityRenderState state = dispatcher.extractEntity(entity, partial);
                    dispatcher.submit(state, camState, state.x - camPos.x, state.y - camPos.y,
                            state.z - camPos.z, poseStack, featureDispatcher.getSubmitNodeStorage());
                } catch (Exception e) {
                    // Never let one broken entity renderer (called outside its usual pass) kill the frame.
                }
            }

            submitBlockEntities(mc, beDispatcher, featureDispatcher, camState, camPos, poseStack);

            featureRenderSafely(featureDispatcher, bufferSource);
        } finally {
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            mvStack.popMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
    }

    // Block entities (chests, banners, signs) hang off the section meshes we already collected, so the
    // caster set matches the terrain cull. Extracted and submitted like vanilla's submitBlockEntities.
    private void submitBlockEntities(Minecraft mc, BlockEntityRenderDispatcher beDispatcher,
                                     FeatureRenderDispatcher featureDispatcher, CameraRenderState camState,
                                     Vec3 camPos, PoseStack poseStack) {
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (SectionRenderDispatcher.RenderSection section : shadowSections) {
            for (BlockEntity be : section.getSectionMesh().getRenderableBlockEntities()) {
                BlockPos pos = be.getBlockPos();
                poseStack.pushPose();
                poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
                try {
                    var state = beDispatcher.tryExtractRenderState(be, partial, null);
                    if (state != null) {
                        beDispatcher.submit(state, poseStack, featureDispatcher.getSubmitNodeStorage(), camState);
                    }
                } catch (Exception e) {
                    // Never let one broken block-entity renderer kill the frame.
                }
                poseStack.popPose();
            }
        }
    }

    // A caught-but-partial submit above can leave a broken node behind; guard the flush so a throw
    // can't escape and skip the global-state restore in the caller's finally.
    private static void featureRenderSafely(FeatureRenderDispatcher featureDispatcher,
                                            MultiBufferSource.BufferSource bufferSource) {
        try {
            featureDispatcher.renderAllFeatures();
            bufferSource.endBatch();
        } catch (Exception e) {
            Polytone.LOGGER.error("Error flushing polytone shadow entity batch", e);
        }
    }

    // Every compiled section intersecting the light volume, regardless of camera visibility (so
    // off-screen occluders still cast). Separating-axis test on the three light-space axes.
    private void collectShadowSections(Minecraft mc, Matrix4f lightView, Vec3 camPos) {
        shadowSections.clear();
        ViewArea viewArea = ((LevelRendererShadowAccessor) mc.levelRenderer).polytone$getViewArea();
        if (viewArea == null) return;

        float coverage = settings.coverage();
        float depthRange = settings.depthRange();

        // A 16^3 section's half-extent (8 per axis) projected onto each light axis (rows of lightView).
        float radX = 8f * (Math.abs(lightView.m00()) + Math.abs(lightView.m10()) + Math.abs(lightView.m20()));
        float radY = 8f * (Math.abs(lightView.m01()) + Math.abs(lightView.m11()) + Math.abs(lightView.m21()));
        float radZ = 8f * (Math.abs(lightView.m02()) + Math.abs(lightView.m12()) + Math.abs(lightView.m22()));

        Vector3f center = new Vector3f();
        for (SectionRenderDispatcher.RenderSection section : viewArea.sections) {
            if (!section.getSectionMesh().hasRenderableLayers()) continue;

            BlockPos origin = section.getRenderOrigin();
            center.set((float) (origin.getX() + 8 - camPos.x),
                    (float) (origin.getY() + 8 - camPos.y),
                    (float) (origin.getZ() + 8 - camPos.z));
            if (insideLightBox(lightView, center, coverage, depthRange, radX, radY, radZ)) {
                shadowSections.add(section);
            }
        }
    }

    // Box-test a camera-relative point against the light's ortho volume (half-extents coverage on the
    // two lateral axes, depthRange along the light axis), with per-axis slack for the object's size.
    // transformDirection rotates the point into light space in place - the multiply-adds are the three
    // separating-axis dot products. Conservative: never a false "outside".
    private static boolean insideLightBox(Matrix4f lightView, Vector3f point, float coverage, float depthRange,
                                          float slackX, float slackY, float slackZ) {
        lightView.transformDirection(point);
        return Math.abs(point.x) <= coverage + slackX
                && Math.abs(point.y) <= coverage + slackY
                && Math.abs(point.z) <= depthRange + slackZ;
    }

    // Direction toward the light, matching vanilla's sun placement in the sky pass: (-sin a, cos a, 0)
    // with a = the SUN_ANGLE environment attribute (straight up at noon, travelling east-up-west),
    // the successor of the old getSunAngle. Continuous (no stepping); respects dimension/datapack
    // overrides. Below the horizon we flip to the moon on the opposite side.
    private static Vector3f computeLightDir(Camera cam, float partial) {
        float a = cam.attributeProbe().getValue(EnvironmentAttributes.SUN_ANGLE, partial) * Mth.DEG_TO_RAD;
        Vector3f sunDir = new Vector3f(-Mth.sin(a), Mth.cos(a), 0f); // already unit length
        if (sunDir.y < 0f) sunDir.negate(); // sun below horizon -> moonlight from the opposite side
        return sunDir;
    }

    private void ensureTarget() {
        GpuDevice device = RenderSystem.getDevice();
        if (projectionBuffer == null) {
            projectionBuffer = device.createBuffer(() -> "Polytone shadow projection UBO",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
        }
        // Recreate when the configured resolution changes (a shadow_map.json reload can move it).
        int shadowRes = settings.resolution();
        if (depthTexture == null || depthTexture.getWidth(0) != shadowRes) {
            closeTextures();
            depthTexture = device.createTexture(() -> "Polytone shadow map depth",
                    GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING,
                    TextureFormat.DEPTH32, shadowRes, shadowRes, 1, 1);
            depthTextureView = device.createTextureView(depthTexture);
            colorTexture = device.createTexture(() -> "Polytone shadow map color",
                    GpuTexture.USAGE_RENDER_ATTACHMENT,
                    TextureFormat.RGBA8, shadowRes, shadowRes, 1, 1);
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
        if (projectionBuffer != null) {
            projectionBuffer.close();
            projectionBuffer = null;
        }
        if (uniforms != null) {
            uniforms.close();
            uniforms = null;
        }
        shadowSections.clear();
        hasRendered = false;
    }
}
