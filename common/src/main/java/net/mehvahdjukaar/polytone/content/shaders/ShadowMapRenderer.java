package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.shaders.sodium.SodiumShadowRenderer;
import net.mehvahdjukaar.polytone.mixins.accessor.LevelRendererShadowAccessor;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Generates a directional (sun/moon) shadow depth map by replaying the level's already-compiled chunk
// VBOs from the light's point of view - nothing is re-meshed, we just point the terrain matrices at the
// light's ortho view and redraw. Exposed to post chains as the InShadow sampler and PolyShadowMat
// uniform (see PostChainEffect). The rendering half of the shadow system; ShadowMapManager owns the
// reloadable settings and feeds them here via setSettings().
public class ShadowMapRenderer {

    // Active parameters, pushed in by ShadowMapManager on reload (see ShadowMapSettings).
    private ShadowMapSettings settings = ShadowMapSettings.DEFAULT;

    // Reuse state: when we skip a re-render, the last map is kept and only re-aligned for camera motion.
    private final Matrix4f renderedMatrix = new Matrix4f();  // shadowMatrix at the last real render
    private Vec3 renderedCamPos = Vec3.ZERO;                 // camera position at the last real render
    private long lastUpdateMs = 0L;
    private boolean hasRendered = false;

    private TextureTarget shadowTarget = null;
    private final Matrix4f shadowMatrix = new Matrix4f();  // light view-proj -> PolyShadowMat
    private final Vector3f lightDir = new Vector3f(0, 1, 0);  // toward the light -> PolyShadowLightDir
    private final Vector3f camFract = new Vector3f();  // fract(camPos) -> PolyShadowCamFract (grid anchor)
    private final List<SectionRenderDispatcher.RenderSection> shadowSections = new ArrayList<>();

    // Swap in freshly reloaded settings; force a fresh render (and lazy target rebuild) on the next frame.
    public void setSettings(ShadowMapSettings settings) {
        this.settings = settings;
        this.hasRendered = false;
    }

    public int getShadowTextureId() {
        return shadowTarget == null ? 0 : shadowTarget.getDepthTextureId();
    }

    public Matrix4f getShadowMatrix() {
        return shadowMatrix;
    }

    public Vector3f getLightDir() {
        return lightDir;
    }

    public Vector3f getCamFract() {
        return camFract;
    }

    // Called from LevelRenderer.renderLevel TAIL (chunk VBOs still current); skips work unless some
    // active post chain declares use_shadow_map.
    public void renderShadowPassIfNeeded() {
        if (!Polytone.POST_SHADERS.anyActiveEffectUsesShadowMap()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Camera cam = mc.gameRenderer.getMainCamera();
        if (level == null || !cam.isInitialized()) return;

        Vec3 camPos = cam.getPosition();
        // fract(camPos) every frame: the resolve shader's world-grid snap must track the live camera
        // even on frames where we reuse the map.
        camFract.set((float) Mth.frac(camPos.x), (float) Mth.frac(camPos.y), (float) Mth.frac(camPos.z));

        long now = Util.getMillis();
        float updateInterval = settings.updateInterval();
        boolean due = !hasRendered || updateInterval <= 0f || (now - lastUpdateMs) >= updateInterval * 50f;
        if (due) {
            float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
            try {
                render(mc, level, cam, camPos, partial);
            } catch (Exception e) {
                // render() restores its own GL state in finally blocks; swallow here so a failed shadow
                // pass can never propagate into renderLevel or leave the frame half-rendered.
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
    }

    private void render(Minecraft mc, ClientLevel level, Camera cam, Vec3 camPos, float partial) {
        ensureTarget();

        float coverage = settings.coverage();
        float depthRange = settings.depthRange();
        int shadowRes = settings.resolution();

        // Light direction: unit vector pointing FROM the scene TOWARD the sun (or moon at night).
        lightDir.set(computeLightDir(level, partial));
        Vector3f up = Math.abs(lightDir.y) > 0.99f ? new Vector3f(0, 0, 1) : new Vector3f(0, 1, 0);

        // Built in camera-relative space (camera at origin) to match the ChunkOffset = origin - camera
        // that the terrain shader consumes per section. A single ortho cascade centered on the camera.
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

        RenderTarget main = mc.getMainRenderTarget();

        // Snapshot the global projection we're about to stomp. Restored in the finally no matter what
        // throws (or what Sodium's terrain replay leaves behind), so the rest of the frame - and every
        // later frame - keeps the camera's view instead of the light's.
        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting savedSorting = RenderSystem.getVertexSorting();

        // Always clear to far depth (1.0), even when there's nothing to draw - a stale map would
        // shadow the world with last frame's (differently-projected) depth.
        shadowTarget.clear(Minecraft.ON_OSX);
        shadowTarget.bindWrite(true);
        try {
            // Replay opaque geometry from the light's POV. Translucent is intentionally skipped (a shadow
            // map stores opaque occluder depth).
            if (!shadowSections.isEmpty()) {
                // Vanilla pipeline: our own loop over the whole section grid, culled against the light
                // volume - occluders behind/off-screen still cast shadows.
                drawLayer(mc, RenderType.solid(), camPos, lightView, lightProj);
                drawLayer(mc, RenderType.cutoutMipped(), camPos, lightView, lightProj);
                drawLayer(mc, RenderType.cutout(), camPos, lightView, lightProj);
            } else {
                // No compiled vanilla sections -> Sodium has replaced the chunk pipeline. All of the
                // Sodium-specific replay (re-cull against the light volume, redraw, restore) is isolated
                // in SodiumShadowRenderer so this class stays free of Sodium types.
                SodiumShadowRenderer.replayTerrain(mc, cam, camPos, lightView, lightProj, coverage, depthRange);
            }

            // Entities are not part of the chunk VBOs - re-dispatch them with the light matrices.
            drawEntities(mc, level, camPos, lightView, lightProj);
        } finally {
            // Restore the main target (+ its full-window viewport) and the camera projection for the
            // rest of the frame (hand, HUD) and every frame after.
            main.bindWrite(true);
            RenderSystem.setProjectionMatrix(savedProj, savedSorting);
            RenderSystem.applyModelViewMatrix();
        }
    }

    // Entities aren't in the chunk VBOs, so dispatch them separately with the light matrices swapped
    // onto the RenderSystem globals (as vanilla renderLevel does), light-volume culled. Runs on both
    // paths; unlike vanilla we include the camera entity so the player casts a shadow in first person.
    private void drawEntities(Minecraft mc, ClientLevel level, Vec3 camPos,
                              Matrix4f lightView, Matrix4f lightProj) {
        float coverage = settings.coverage();
        float depthRange = settings.depthRange();

        var dispatcher = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // Swap the light matrices onto the RenderSystem globals. The pushMatrix()/popMatrix() pair MUST
        // stay balanced across any throw - getModelViewStack() is a persistent global, so an unbalanced
        // push leaks the light basis into later frames and renders the world from the sun's POV. render()
        // restores the projection, so we only own the model-view stack here.
        Matrix4fStack mvStack = RenderSystem.getModelViewStack();
        mvStack.pushMatrix();
        try {
            mvStack.mul(lightView);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(lightProj, VertexSorting.ORTHOGRAPHIC_Z);

            // Light-volume cull, same separating-axis idea as the sections but with a scalar radius.
            PoseStack poseStack = new PoseStack();
            Vector3f center = new Vector3f();
            for (Entity entity : level.entitiesForRendering()) {
                if (entity.isSpectator()) continue;

                AABB bb = entity.getBoundingBox();
                float radius = (float) Math.max(bb.getXsize(), Math.max(bb.getYsize(), bb.getZsize()));
                Vec3 c = bb.getCenter();
                center.set((float) (c.x - camPos.x), (float) (c.y - camPos.y), (float) (c.z - camPos.z));
                if (!insideLightBox(lightView, center, coverage, depthRange, radius, radius, radius)) continue;

                float partial = mc.getTimer().getGameTimeDeltaPartialTick(
                        !level.tickRateManager().isEntityFrozen(entity));
                double x = Mth.lerp(partial, entity.xOld, entity.getX());
                double y = Mth.lerp(partial, entity.yOld, entity.getY());
                double z = Mth.lerp(partial, entity.zOld, entity.getZ());
                float yaw = Mth.lerp(partial, entity.yRotO, entity.getYRot());
                try {
                    // Fullbright light: only depth matters here, skip the per-entity light lookup.
                    dispatcher.render(entity, x - camPos.x, y - camPos.y, z - camPos.z, yaw,
                            partial, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
                } catch (Exception e) {
                    // Never let one broken entity renderer (called outside its usual pass) kill the frame.
                }
            }

            drawBlockEntities(mc, level, camPos, bufferSource, poseStack, lightView);

            // A caught-but-partial entity render above can leave the shared buffer half-written; guard
            // the flush so it can't escape and skip the matrix restore below.
            try {
                bufferSource.endBatch();
            } catch (Exception e) {
                Polytone.LOGGER.error("Error flushing polytone shadow entity batch", e);
            }
        } finally {
            mvStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    // Block entities (chests, banners, signs) are in neither the chunk VBOs nor the entity list, so
    // dispatch them too - read straight off the loaded chunks (works on vanilla and Sodium alike),
    // light-volume culled, into the same batch drawEntities already set up. Only the chunks within
    // lateral coverage are scanned, so a far occluder under a low sun is skipped (its shadow would
    // land past the cascade anyway).
    private void drawBlockEntities(Minecraft mc, ClientLevel level, Vec3 camPos,
                                   MultiBufferSource bufferSource, PoseStack poseStack, Matrix4f lightView) {
        float coverage = settings.coverage();
        float depthRange = settings.depthRange();

        BlockEntityRenderDispatcher beDispatcher = mc.getBlockEntityRenderDispatcher();
        float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);

        int camChunkX = Mth.floor(camPos.x) >> 4;
        int camChunkZ = Mth.floor(camPos.z) >> 4;
        int chunkRadius = Mth.ceil(coverage / 16f) + 1;
        float radius = 1.5f; // most block entities fit in a block; slack for taller ones (beds, chests)
        Vector3f center = new Vector3f();

        for (int cx = camChunkX - chunkRadius; cx <= camChunkX + chunkRadius; cx++) {
            for (int cz = camChunkZ - chunkRadius; cz <= camChunkZ + chunkRadius; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();
                    center.set((float) (pos.getX() + 0.5 - camPos.x),
                            (float) (pos.getY() + 0.5 - camPos.y),
                            (float) (pos.getZ() + 0.5 - camPos.z));
                    if (!insideLightBox(lightView, center, coverage, depthRange, radius, radius, radius)) continue;

                    poseStack.pushPose();
                    poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
                    try {
                        beDispatcher.render(entry.getValue(), partial, poseStack, bufferSource);
                    } catch (Exception e) {
                        // Never let one broken block-entity renderer kill the frame.
                    }
                    poseStack.popPose();
                }
            }
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
            SectionRenderDispatcher.CompiledSection compiled = section.getCompiled();
            if (compiled == SectionRenderDispatcher.CompiledSection.UNCOMPILED
                    || compiled.hasNoRenderableLayers()) continue;

            BlockPos origin = section.getOrigin();
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
    // separating-axis dot products we used to spell out by hand. Conservative: never a false "outside".
    private static boolean insideLightBox(Matrix4f lightView, Vector3f point, float coverage, float depthRange,
                                          float slackX, float slackY, float slackZ) {
        lightView.transformDirection(point);
        return Math.abs(point.x) <= coverage + slackX
                && Math.abs(point.y) <= coverage + slackY
                && Math.abs(point.z) <= depthRange + slackZ;
    }

    // One terrain layer of the collected sections with the light matrices - the body of vanilla's
    // renderSectionLayer minus translucency sorting, profiler and mod render-stage hooks.
    private void drawLayer(Minecraft mc, RenderType renderType, Vec3 camPos,
                           Matrix4f lightView, Matrix4f lightProj) {
        renderType.setupRenderState();
        ShaderInstance shader = RenderSystem.getShader();
        if (shader == null) { // no bound program (another mod cleared it?) - nothing we can draw
            renderType.clearRenderState();
            return;
        }
        try {
            shader.setDefaultUniforms(VertexFormat.Mode.QUADS, lightView, lightProj, mc.getWindow());
            shader.apply();
            Uniform chunkOffset = shader.CHUNK_OFFSET;

            for (SectionRenderDispatcher.RenderSection section : shadowSections) {
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

    // Direction toward the light, matching vanilla's sun placement in renderSky: (-sin a, cos a, 0)
    // with a = getSunAngle (straight up at noon, travelling east-up-west). Continuous (no stepping).
    // Below the horizon we flip to the moon on the opposite side.
    private static Vector3f computeLightDir(ClientLevel level, float partial) {
        float a = level.getSunAngle(partial);
        Vector3f sunDir = new Vector3f(-Mth.sin(a), Mth.cos(a), 0f); // already unit length
        if (sunDir.y < 0f) sunDir.negate(); // sun below horizon -> moonlight from the opposite side
        return sunDir;
    }

    private void ensureTarget() {
        // Recreate when the configured resolution changes (a shadow_map.json reload can move it).
        int shadowRes = settings.resolution();
        if (shadowTarget == null || shadowTarget.width != shadowRes) {
            if (shadowTarget != null) shadowTarget.destroyBuffers();
            shadowTarget = new TextureTarget(shadowRes, shadowRes, true, Minecraft.ON_OSX);
            shadowTarget.setClearColor(1f, 1f, 1f, 1f);
        }
    }

    public void close() {
        if (shadowTarget != null) {
            shadowTarget.destroyBuffers();
            shadowTarget = null;
        }
        shadowSections.clear();
    }
}
