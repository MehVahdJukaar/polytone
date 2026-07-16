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
import net.mehvahdjukaar.polytone.mixins.accessor.LevelRendererShadowAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates a directional (sun/moon) shadow depth map by replaying the level's already-compiled
 * chunk section VBOs from the light's point of view.
 *
 * <p><b>How it works.</b> Nothing is re-meshed: every compiled {@code RenderSection} already owns a
 * {@link VertexBuffer} per render type, and the terrain shader positions it with just a per-section
 * {@code ChunkOffset} (camera-relative section origin) plus the view/projection matrices. So the
 * shadow pass binds a depth framebuffer, points those matrices at the light's orthographic view and
 * redraws the buffers. The result is exposed to Polytone post chains as the {@code InShadow} sampler
 * plus the {@code PolyShadowMat} light view-projection uniform (see {@link PostChainEffect}).</p>
 *
 * <p><b>Which sections.</b> Deliberately NOT {@code visibleSections}: that list is culled against the
 * camera frustum, so occluders behind/beside the player would be missing from the map and their
 * shadows would pop in and out as the view turns. We iterate the whole {@link ViewArea} grid and cull
 * against the light volume instead. Caveat: sections the camera has never looked at are not compiled
 * by vanilla, so their shadows appear only once they've been on screen at least once.</p>
 *
 * <p><b>Temporal stability.</b> Two measures keep the map from shimmering: the texel grid is anchored
 * to the WORLD ORIGIN (computed in double precision, so it survives large coordinates) rather than to
 * anything camera-relative, and the sun angle is quantized so the light basis - and with it the whole
 * grid - only changes in discrete steps a few times per second instead of continuously rotating.</p>
 *
 * <p>Follow-ups: depth-only shader instead of the full terrain shader, CSM cascades for range, a
 * Sodium path (Sodium bypasses vanilla's section grid entirely).</p>
 */
public class ShadowMapManager {

    /** Shadow map resolution (square). Independent of the window size. */
    private static final int SHADOW_RES = 2048;
    /**
     * Half-width of the orthographic coverage box, in blocks (single cascade centered on the camera).
     * 2048 / 128 = 16 shadow texels per block, a 1:1 match with default 16px block textures.
     */
    private static final float COVERAGE = 64f;
    /** Half-depth of the ortho box along the light axis, in blocks. */
    private static final float DEPTH_RANGE = 256f;
    /**
     * The sun angle is quantized to this many steps per day so the light basis stays constant between
     * steps (a continuously rotating grid crawls). 2048 steps = one step every ~12 ticks (~0.6 s).
     */
    private static final int SUN_ANGLE_STEPS = 2048;

    private TextureTarget shadowTarget = null;
    /** Light view-projection (camera-relative space), matching the coordinate space of the depth map. */
    private final Matrix4f shadowMatrix = new Matrix4f();
    /** Unit direction toward the light this frame; exposed to the resolve pass for slope/normal-offset bias. */
    private final Vector3f lightDir = new Vector3f(0, 1, 0);
    /**
     * Fractional part of the camera position this frame ({@code camPos - floor(camPos)}), exposed as
     * {@code PolyShadowCamFract} so the resolve pass can snap camera-relative positions to a
     * world-block-aligned grid (the pixelated-shadows look).
     */
    private final Vector3f camFract = new Vector3f();
    /** Scratch list of sections that intersect the light volume this frame. */
    private final List<SectionRenderDispatcher.RenderSection> shadowSections = new ArrayList<>();

    /** Depth texture id of the shadow map, or 0 if not yet rendered. Bound as the {@code InShadow} sampler. */
    public int getShadowTextureId() {
        return shadowTarget == null ? 0 : shadowTarget.getDepthTextureId();
    }

    /** Light view-projection for the current frame, exposed as the {@code PolyShadowMat} uniform. */
    public Matrix4f getShadowMatrix() {
        return shadowMatrix;
    }

    /** Unit direction toward the light this frame, exposed as the {@code PolyShadowLightDir} uniform. */
    public Vector3f getLightDir() {
        return lightDir;
    }

    /** Camera-position fractional part this frame, exposed as the {@code PolyShadowCamFract} uniform. */
    public Vector3f getCamFract() {
        return camFract;
    }

    /**
     * Render the shadow depth map for this frame, but only if some active post chain declares
     * {@code use_shadow_map}. Called from {@code LevelRenderer.renderLevel} TAIL, alongside the
     * depth snapshot - while the chunk VBOs are still current for this frame.
     */
    public void renderShadowPassIfNeeded() {
        if (!Polytone.POST_SHADERS.anyActiveEffectUsesShadowMap()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Camera cam = mc.gameRenderer.getMainCamera();
        if (level == null || !cam.isInitialized()) return;

        float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
        render(mc, level, cam, partial);
    }

    private void render(Minecraft mc, ClientLevel level, Camera cam, float partial) {
        ensureTarget();

        Vec3 camPos = cam.getPosition();
        camFract.set(
                (float) (camPos.x - Math.floor(camPos.x)),
                (float) (camPos.y - Math.floor(camPos.y)),
                (float) (camPos.z - Math.floor(camPos.z)));

        // Light direction: unit vector pointing FROM the scene TOWARD the sun (or moon at night).
        lightDir.set(computeLightDir(level, partial));
        Vector3f up = Math.abs(lightDir.y) > 0.99f ? new Vector3f(0, 0, 1) : new Vector3f(0, 1, 0);

        // Built in camera-relative space (camera at origin) to match the ChunkOffset = origin - camera
        // that the terrain shader consumes per section. A single ortho cascade centered on the camera.
        Matrix4f lightView = new Matrix4f().lookAlong(
                -lightDir.x, -lightDir.y, -lightDir.z, up.x, up.y, up.z);
        Matrix4f lightProj = new Matrix4f().ortho(
                -COVERAGE, COVERAGE, -COVERAGE, COVERAGE, -DEPTH_RANGE, DEPTH_RANGE);

        // Texel snapping, anchored at the WORLD ORIGIN so the grid never re-phases as the camera
        // moves. (Anchoring to anything that changes with the camera - e.g. the block corner under
        // it - makes the grid jump when the anchor does, because a 1-block world step projects to a
        // non-integer number of oblique shadow texels.) s = light-space x/y of the camera's absolute
        // world position; the projection is nudged so world points always land on the same texel
        // phase. Double precision keeps the mod-texel result exact at large coordinates.
        double sx = lightView.m00() * camPos.x + lightView.m10() * camPos.y + lightView.m20() * camPos.z;
        double sy = lightView.m01() * camPos.x + lightView.m11() * camPos.y + lightView.m21() * camPos.z;
        double texel = 2.0 * COVERAGE / SHADOW_RES; // world-space size of one shadow texel
        lightProj.m30(lightProj.m30() + (float) ((sx - Math.round(sx / texel) * texel) / COVERAGE));
        lightProj.m31(lightProj.m31() + (float) ((sy - Math.round(sy / texel) * texel) / COVERAGE));

        shadowMatrix.set(lightProj).mul(lightView);

        collectShadowSections(mc, lightView, camPos);

        RenderTarget main = mc.getMainRenderTarget();

        // Always clear to far depth (1.0), even when there's nothing to draw - a stale map would
        // shadow the world with last frame's (differently-projected) depth.
        shadowTarget.clear(Minecraft.ON_OSX);
        shadowTarget.bindWrite(true);

        // Replay opaque geometry from the light's POV. Translucent is intentionally skipped (a shadow
        // map stores opaque occluder depth).
        if (!shadowSections.isEmpty()) {
            // Vanilla pipeline: our own loop over the whole section grid, culled against the light
            // volume - occluders behind/off-screen still cast shadows.
            drawLayer(mc, RenderType.solid(), camPos, lightView, lightProj);
            drawLayer(mc, RenderType.cutoutMipped(), camPos, lightView, lightProj);
            drawLayer(mc, RenderType.cutout(), camPos, lightView, lightProj);
        } else {
            // No compiled vanilla sections -> Sodium has replaced the chunk pipeline (its terrain
            // never touches the vanilla grid). Sodium @Overwrites renderSectionLayer and forwards
            // whatever matrices we pass to its own drawChunkLayer, so invoking it replays Sodium's
            // terrain with the light matrices. Its render lists are camera-frustum culled though,
            // so off-screen occluders don't cast shadows on this path (shadow pop when turning).
            LevelRendererShadowAccessor lr = (LevelRendererShadowAccessor) mc.levelRenderer;
            lr.polytone$renderSectionLayer(RenderType.solid(), camPos.x, camPos.y, camPos.z, lightView, lightProj);
            lr.polytone$renderSectionLayer(RenderType.cutoutMipped(), camPos.x, camPos.y, camPos.z, lightView, lightProj);
            lr.polytone$renderSectionLayer(RenderType.cutout(), camPos.x, camPos.y, camPos.z, lightView, lightProj);
        }

        // Entities are not part of the chunk VBOs - re-dispatch them with the light matrices.
        drawEntities(mc, level, camPos, lightView, lightProj);

        // Restore the main target AND its full-window viewport for the rest of the frame (hand, HUD).
        main.bindWrite(true);
    }

    /**
     * Render entities into the shadow map. Mirrors how {@code LevelRenderer.renderLevel} draws them:
     * the view matrix lives on the {@code RenderSystem} model-view stack (with the projection on the
     * matching global), the {@code PoseStack} starts at identity and coordinates are camera-relative.
     * We temporarily swap both globals for the light matrices, dispatch every entity that intersects
     * the light volume (light-culled, NOT camera-culled), flush the batch, and restore. Runs on both
     * the vanilla and Sodium paths (entities are vanilla-rendered either way). Unlike vanilla we also
     * include the camera entity in first person, so the player casts a shadow.
     */
    private void drawEntities(Minecraft mc, ClientLevel level, Vec3 camPos,
                              Matrix4f lightView, Matrix4f lightProj) {
        var dispatcher = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting savedSorting = RenderSystem.getVertexSorting();
        Matrix4fStack mvStack = RenderSystem.getModelViewStack();
        mvStack.pushMatrix();
        mvStack.mul(lightView);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(lightProj, VertexSorting.ORTHOGRAPHIC_Z);

        // Light-volume cull, same separating-axis idea as the sections but with a scalar radius.
        float r00 = lightView.m00(), r10 = lightView.m10(), r20 = lightView.m20();
        float r01 = lightView.m01(), r11 = lightView.m11(), r21 = lightView.m21();
        float r02 = lightView.m02(), r12 = lightView.m12(), r22 = lightView.m22();

        PoseStack poseStack = new PoseStack();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity.isSpectator()) continue;

            AABB bb = entity.getBoundingBox();
            float radius = (float) Math.max(bb.getXsize(), Math.max(bb.getYsize(), bb.getZsize()));
            float cx = (float) ((bb.minX + bb.maxX) * 0.5 - camPos.x);
            float cy = (float) ((bb.minY + bb.maxY) * 0.5 - camPos.y);
            float cz = (float) ((bb.minZ + bb.maxZ) * 0.5 - camPos.z);
            if (Math.abs(r00 * cx + r10 * cy + r20 * cz) > COVERAGE + radius) continue;
            if (Math.abs(r01 * cx + r11 * cy + r21 * cz) > COVERAGE + radius) continue;
            if (Math.abs(r02 * cx + r12 * cy + r22 * cz) > DEPTH_RANGE + radius) continue;

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
        bufferSource.endBatch();

        mvStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(savedProj, savedSorting);
    }

    /**
     * Gather every compiled section whose bounds intersect the light's orthographic volume,
     * regardless of camera-frustum visibility. Separating-axis test: project the section center onto
     * the three light-space axes (the rows of the rotation-only light view matrix) and compare
     * against the box half-extents inflated by the section's own projected radius.
     */
    private void collectShadowSections(Minecraft mc, Matrix4f lightView, Vec3 camPos) {
        shadowSections.clear();
        ViewArea viewArea = ((LevelRendererShadowAccessor) mc.levelRenderer).polytone$getViewArea();
        if (viewArea == null) return;

        float r00 = lightView.m00(), r10 = lightView.m10(), r20 = lightView.m20(); // light X axis
        float r01 = lightView.m01(), r11 = lightView.m11(), r21 = lightView.m21(); // light Y axis
        float r02 = lightView.m02(), r12 = lightView.m12(), r22 = lightView.m22(); // light Z axis
        // A 16^3 section's half-extent (8 per axis) projected onto each light axis.
        float radX = 8f * (Math.abs(r00) + Math.abs(r10) + Math.abs(r20));
        float radY = 8f * (Math.abs(r01) + Math.abs(r11) + Math.abs(r21));
        float radZ = 8f * (Math.abs(r02) + Math.abs(r12) + Math.abs(r22));

        for (SectionRenderDispatcher.RenderSection section : viewArea.sections) {
            SectionRenderDispatcher.CompiledSection compiled = section.getCompiled();
            if (compiled == SectionRenderDispatcher.CompiledSection.UNCOMPILED
                    || compiled.hasNoRenderableLayers()) continue;

            BlockPos origin = section.getOrigin();
            float cx = (float) (origin.getX() + 8 - camPos.x);
            float cy = (float) (origin.getY() + 8 - camPos.y);
            float cz = (float) (origin.getZ() + 8 - camPos.z);

            if (Math.abs(r00 * cx + r10 * cy + r20 * cz) > COVERAGE + radX) continue;
            if (Math.abs(r01 * cx + r11 * cy + r21 * cz) > COVERAGE + radY) continue;
            if (Math.abs(r02 * cx + r12 * cy + r22 * cz) > DEPTH_RANGE + radZ) continue;
            shadowSections.add(section);
        }
    }

    /**
     * Draw one terrain layer of the collected sections with the light matrices. Mirrors the body of
     * vanilla's {@code LevelRenderer.renderSectionLayer} (state setup, default uniforms, per-section
     * {@code ChunkOffset}), minus the translucency sorting, profiler and mod render-stage hooks.
     */
    private void drawLayer(Minecraft mc, RenderType renderType, Vec3 camPos,
                           Matrix4f lightView, Matrix4f lightProj) {
        renderType.setupRenderState();
        ShaderInstance shader = RenderSystem.getShader();
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
        shader.clear();
        VertexBuffer.unbind();
        renderType.clearRenderState();
    }

    /**
     * Unit direction toward the light, matching exactly how vanilla places the sun in
     * {@code LevelRenderer.renderSky}: the sun quad sits at local {@code (0,100,0)} after a
     * {@code Y(-90deg)} then {@code X(getSunAngle)} rotation, which resolves to a world direction of
     * {@code (-sin a, cos a, 0)} with {@code a = getSunAngle} (straight up at noon; vanilla's sun
     * travels purely east-up-west). The angle is quantized to {@link #SUN_ANGLE_STEPS} so shadows
     * step discretely instead of crawling continuously. Once the sun drops below the horizon we flip
     * to the moon on the opposite side.
     */
    private static Vector3f computeLightDir(ClientLevel level, float partial) {
        float a = level.getSunAngle(partial);
        a = (float) (Math.round((double) a * SUN_ANGLE_STEPS / (Math.PI * 2)) * (Math.PI * 2) / SUN_ANGLE_STEPS);
        Vector3f sunDir = new Vector3f(-Mth.sin(a), Mth.cos(a), 0f); // already unit length
        if (sunDir.y < 0f) sunDir.negate(); // sun below horizon -> moonlight from the opposite side
        return sunDir;
    }

    private void ensureTarget() {
        if (shadowTarget == null) {
            shadowTarget = new TextureTarget(SHADOW_RES, SHADOW_RES, true, Minecraft.ON_OSX);
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
