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
    private ClientLevel renderedLevel = null;                // level of the last real render
    private long lastUpdateMs = 0L;
    private boolean hasRendered = false;

    // Set for the duration of a pass. The pass dispatches entity and block-entity renderers, and mods
    // render entire levels from those - a nested LevelRenderer.renderLevel landing back here would
    // clear shadowSections while we iterate it and hand the shadow target's binding to the main one.
    private boolean insidePass = false;

    private TextureTarget shadowTarget = null;
    private final Matrix4f lightView = new Matrix4f();     // camera-relative world -> light space
    private final Matrix4f lightProj = new Matrix4f();     // the ortho light box, texel-snapped
    private final Matrix4f shadowMatrix = new Matrix4f();  // light view-proj -> PolyShadowMat
    private final Vector3f lightDir = new Vector3f(0, 1, 0);  // toward the light -> PolyShadowLightDir
    private final Vector3f camFract = new Vector3f();  // fract(camPos) -> PolyShadowCamFract (grid anchor)
    private final List<SectionRenderDispatcher.RenderSection> shadowSections = new ArrayList<>();

    // Depth-only replacements for the vanilla terrain programs, loaded lazily on first use. Null (with
    // the failed flag set) means we fall back to whatever RenderType.setupRenderState bound.
    private ShaderInstance opaqueDepthShader = null;
    private ShaderInstance cutoutDepthShader = null;
    private boolean depthShadersFailed = false;

    // Swap in freshly reloaded settings; force a fresh render (and lazy target rebuild) on the next frame.
    public void setSettings(ShadowMapSettings settings) {
        this.settings = settings;
        this.hasRendered = false;
        // Also the disconnect path (resetWithLevel), so the cache can't pin a dead ClientLevel.
        this.renderedLevel = null;
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
    // active post chain declares use_shadow_map. The camera matrices are renderLevel's own, and are
    // camera-relative, which is the space the caster-volume cull works in.
    public void renderShadowPassIfNeeded(Camera cam, Matrix4f cameraFrustumMatrix, Matrix4f cameraProjectionMatrix) {
        if (insidePass) return;
        if (!Polytone.POST_SHADERS.anyActiveEffectUsesShadowMap()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        // The camera comes from renderLevel's own parameter rather than gameRenderer.getMainCamera():
        // that field is a global other mods swap while they render a second view, and it has to agree
        // with the frustum matrices above - they are all camera-relative to the same eye.
        if (level == null || !cam.isInitialized()) return;

        Vec3 camPos = cam.getPosition();
        // fract(camPos) every frame: the resolve shader's world-grid snap must track the live camera
        // even on frames where we reuse the map.
        camFract.set((float) Mth.frac(camPos.x), (float) Mth.frac(camPos.y), (float) Mth.frac(camPos.z));

        long now = Util.getMillis();
        float updateInterval = settings.updateInterval();
        // The re-align below is a translation of a box centered on the camera at render time, so it only
        // holds while the camera is still well inside that box. A teleport, a dimension change or a
        // second view rendered from somewhere else entirely would otherwise slide the whole map away.
        float maxReuseDrift = settings.coverage() * 0.25f;
        boolean reusable = hasRendered && level == renderedLevel
                && camPos.distanceToSqr(renderedCamPos) <= maxReuseDrift * maxReuseDrift;
        boolean due = !reusable || updateInterval <= 0f || (now - lastUpdateMs) >= updateInterval * 50f;
        if (due) {
            float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
            boolean ok = true;
            insidePass = true;
            try {
                render(mc, level, cam, camPos, partial, cameraFrustumMatrix, cameraProjectionMatrix);
            } catch (Exception e) {
                // render() restores its own GL state in finally blocks; swallow here so a failed shadow
                // pass can never propagate into renderLevel or leave the frame half-rendered.
                ok = false;
                Polytone.LOGGER.error("Polytone shadow-map render failed", e);
            } finally {
                insidePass = false;
            }
            // Only a completed pass may be reused: caching a half-drawn map would keep it on screen for
            // the whole update interval.
            if (ok) {
                renderedMatrix.set(shadowMatrix);
                renderedCamPos = camPos;
                renderedLevel = level;
                lastUpdateMs = now;
                hasRendered = true;
            } else {
                hasRendered = false;
            }
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

    private void render(Minecraft mc, ClientLevel level, Camera cam, Vec3 camPos, float partial,
                        Matrix4f cameraFrustumMatrix, Matrix4f cameraProjectionMatrix) {
        ensureTarget();
        updateLightMatrices(level, camPos, partial);

        // The caster set: the light box, narrowed to what can actually shadow the view frustum. The
        // narrowing is only sound while the map is rendered every frame - with reuse enabled the camera
        // turns between renders and would look into parts of the map we never filled, so we drop back to
        // the plain box there.
        ShadowCasterVolume volume = new ShadowCasterVolume(lightView, settings.coverage(), settings.depthRange());
        if (settings.updateInterval() <= 0f && cameraFrustumMatrix != null && cameraProjectionMatrix != null) {
            volume.buildCasterPlanes(cameraProjectionMatrix.mul(cameraFrustumMatrix, new Matrix4f()), lightDir);
        }

        collectShadowSections(mc, volume, camPos);

        RenderTarget main = mc.getMainRenderTarget();

        // Snapshot the global projection we're about to stomp. Restored in the finally no matter what
        // throws (or what Sodium's terrain replay leaves behind), so the rest of the frame - and every
        // later frame - keeps the camera's view instead of the light's.
        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting savedSorting = RenderSystem.getVertexSorting();

        // Always clear to far depth (1.0), even when there's nothing to draw - a stale map would
        // shadow the world with last frame's (differently-projected) depth. Depth only: the target has
        // no draw buffer bound (see ensureTarget), so a colour clear would write nothing anyway.
        shadowTarget.bindWrite(true);
        RenderSystem.depthMask(true);
        GlStateManager._clearDepth(1.0);
        GlStateManager._clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        try {
            // Replay opaque geometry from the light's POV. Translucent is intentionally skipped (a shadow
            // map stores opaque occluder depth).
            if (!shadowSections.isEmpty()) {
                // Vanilla pipeline: our own loop over the whole section grid, culled against the caster
                // volume - occluders behind/off-screen still cast shadows.
                drawLayer(mc, RenderType.solid(), camPos, depthShader(mc, false));
                drawLayer(mc, RenderType.cutoutMipped(), camPos, depthShader(mc, true));
                drawLayer(mc, RenderType.cutout(), camPos, depthShader(mc, true));
            } else if (CompatHandler.SODIUM) {
                // No compiled vanilla sections -> Sodium has replaced the chunk pipeline. All of the
                // Sodium-specific replay (re-cull against the light volume, redraw, restore) is isolated
                // in SodiumShadowRenderer. The CompatHandler.SODIUM guard MUST stay at this call site:
                // touching SodiumShadowRenderer links it, and the verifier eager-loads its Sodium types
                // (Frustum via SodiumLightVolumeFrustum) - so without this gate the branch crashes with a
                // ClassNotFoundException whenever vanilla simply has no compiled sections (near spawn, sky).
                SodiumShadowRenderer.replayTerrain(mc, cam, camPos, lightView, lightProj, volume);
            }

            // Entities are not part of the chunk VBOs - re-dispatch them with the light matrices.
            if (settings.renderEntities() || settings.renderBlockEntities()) {
                drawEntities(mc, level, camPos, volume);
            }
        } finally {
            // Restore the main target (+ its full-window viewport) and the camera projection for the
            // rest of the frame (hand, HUD) and every frame after.
            main.bindWrite(true);
            RenderSystem.setProjectionMatrix(savedProj, savedSorting);
            RenderSystem.applyModelViewMatrix();
        }
    }

    // Light view, ortho box and their product, all in camera-relative space (camera at the origin) to
    // match the ChunkOffset = origin - camera that the terrain shader consumes per section. A single
    // ortho cascade centered on the camera.
    private void updateLightMatrices(ClientLevel level, Vec3 camPos, float partial) {
        float coverage = settings.coverage();
        float depthRange = settings.depthRange();

        updateLightDir(level, partial);
        // Any up vector does, as long as it isn't parallel to the light.
        boolean lightNearlyVertical = Math.abs(lightDir.y) > 0.99f;
        lightView.setLookAlong(-lightDir.x, -lightDir.y, -lightDir.z,
                0f, lightNearlyVertical ? 0f : 1f, lightNearlyVertical ? 1f : 0f);
        lightProj.setOrtho(-coverage, coverage, -coverage, coverage, -depthRange, depthRange);

        // Texel snap keeps world geometry on the same shadow texels frame to frame (stops edge shimmer).
        // Anchored to the camera's CHUNK corner, not the world origin: the snap offset's sensitivity to a
        // rotating sun scales with distance to the anchor, so a world-origin anchor shimmered badly far
        // from spawn while a chunk-local one (<=16 blocks) does not. Doubles keep the mod exact.
        double anchorX = Mth.positiveModulo(camPos.x, 16.0);
        double anchorY = Mth.positiveModulo(camPos.y, 16.0);
        double anchorZ = Mth.positiveModulo(camPos.z, 16.0);
        double lightSpaceX = lightView.m00() * anchorX + lightView.m10() * anchorY + lightView.m20() * anchorZ;
        double lightSpaceY = lightView.m01() * anchorX + lightView.m11() * anchorY + lightView.m21() * anchorZ;
        double texel = 2.0 * coverage / settings.resolution(); // world-space size of one shadow texel
        lightProj.m30(lightProj.m30() + (float) (offsetToTexelGrid(lightSpaceX, texel) / coverage));
        lightProj.m31(lightProj.m31() + (float) (offsetToTexelGrid(lightSpaceY, texel) / coverage));

        shadowMatrix.set(lightProj).mul(lightView);
    }

    // Signed distance from a light-space coordinate back to the nearest texel boundary.
    private static double offsetToTexelGrid(double lightSpaceCoord, double texel) {
        return lightSpaceCoord - Math.round(lightSpaceCoord / texel) * texel;
    }

    // Direction toward the light, matching vanilla's sun placement in renderSky: (-sin a, cos a, 0) with
    // a = getSunAngle (straight up at noon, travelling east-up-west). Continuous (no stepping). Below the
    // horizon we flip to the moon on the opposite side.
    private void updateLightDir(ClientLevel level, float partial) {
        float sunAngle = level.getSunAngle(partial);
        lightDir.set(-Mth.sin(sunAngle), Mth.cos(sunAngle), 0f); // already unit length
        if (lightDir.y < 0f) lightDir.negate();
    }

    // Entities aren't in the chunk VBOs, so dispatch them separately with the light matrices swapped
    // onto the RenderSystem globals (as vanilla renderLevel does), light-volume culled. Runs on both
    // paths; unlike vanilla we include the camera entity so the player casts a shadow in first person.
    private void drawEntities(Minecraft mc, ClientLevel level, Vec3 camPos, ShadowCasterVolume volume) {
        var dispatcher = mc.getEntityRenderDispatcher();
        // The frame's shared buffer, borrowed empty and handed back empty: renderLevel flushes it in
        // full before the weather block, well before the TAIL we run at, and our endBatch below closes
        // whatever we put in. Anything a mod leaves pending there past that flush would be drawn into
        // the shadow map instead of the screen - a private buffer source is the fix if that ever shows
        // up, together with sorting entities by type (see SHADOW_PERF.md).
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

            // Caster-volume cull, same conservative box test as the sections but with a scalar radius.
            PoseStack poseStack = new PoseStack();
            if (settings.renderEntities()) {
                TickRateManager tickRate = level.tickRateManager();
                float runningPartial = mc.getTimer().getGameTimeDeltaPartialTick(true);
                float frozenPartial = mc.getTimer().getGameTimeDeltaPartialTick(false);

                for (Entity entity : level.entitiesForRendering()) {
                    if (entity.isSpectator()) continue;

                    AABB bb = entity.getBoundingBox();
                    float radius = (float) Math.max(bb.getXsize(), Math.max(bb.getYsize(), bb.getZsize()));
                    if (!castsIntoView(volume, camPos, (bb.minX + bb.maxX) * 0.5,
                            (bb.minY + bb.maxY) * 0.5, (bb.minZ + bb.maxZ) * 0.5, radius)) continue;

                    float partial = tickRate.isEntityFrozen(entity) ? frozenPartial : runningPartial;
                    Vec3 pos = entity.getPosition(partial);
                    try {
                        // Fullbright light: only depth matters here, skip the per-entity light lookup.
                        dispatcher.render(entity, pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z,
                                entity.getViewYRot(partial), partial, poseStack, bufferSource,
                                LightTexture.FULL_BRIGHT);
                    } catch (Exception e) {
                        // Never let one broken entity renderer (called outside its usual pass) kill the frame.
                    }
                }
            }

            if (settings.renderBlockEntities()) {
                drawBlockEntities(mc, level, camPos, bufferSource, poseStack, volume);
            }

            // A caught-but-partial entity render above can leave the shared buffer half-written; guard
            // the flush so it can't escape and skip the matrix restore below.
            try {
                // The batch only reaches the GPU here, so it is this binding that decides where the
                // entities land. Re-assert it: the renderers we just called are mod code running
                // outside its usual pass, and one that renders a level of its own (mirrors, portals)
                // leaves the main framebuffer bound behind it.
                shadowTarget.bindWrite(true);
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
    // dispatch them too, caster-volume culled, into the same batch drawEntities already set up.
    //
    // On the vanilla pipeline the compiled sections we already collected carry the list of block
    // entities that actually have a renderer, so we reuse that instead of walking chunks: it is a far
    // smaller set (a chunk's block-entity map holds every one of them, renderable or not) and it is
    // already narrowed to the caster volume. Sodium leaves the vanilla sections uncompiled, so that path
    // falls back to scanning the loaded chunks within lateral coverage.
    private void drawBlockEntities(Minecraft mc, ClientLevel level, Vec3 camPos,
                                   MultiBufferSource bufferSource, PoseStack poseStack,
                                   ShadowCasterVolume volume) {
        BlockEntityRenderDispatcher beDispatcher = mc.getBlockEntityRenderDispatcher();
        float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
        float radius = 1.5f; // most block entities fit in a block; slack for taller ones (beds, chests)

        if (!shadowSections.isEmpty()) {
            for (SectionRenderDispatcher.RenderSection section : shadowSections) {
                for (BlockEntity be : section.getCompiled().getRenderableBlockEntities()) {
                    renderBlockEntity(beDispatcher, be, camPos, bufferSource, poseStack, volume, radius, partial);
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
                    renderBlockEntity(beDispatcher, entry.getValue(), camPos, bufferSource, poseStack,
                            volume, radius, partial);
                }
            }
        }
    }

    private static void renderBlockEntity(BlockEntityRenderDispatcher beDispatcher, BlockEntity be, Vec3 camPos,
                                          MultiBufferSource bufferSource, PoseStack poseStack,
                                          ShadowCasterVolume volume, float radius, float partial) {
        BlockPos pos = be.getBlockPos();
        if (!castsIntoView(volume, camPos, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, radius)) return;

        poseStack.pushPose();
        poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
        try {
            beDispatcher.render(be, partial, poseStack, bufferSource);
        } catch (Exception e) {
            // Never let one broken block-entity renderer kill the frame.
        }
        poseStack.popPose();
    }

    // Every compiled section that can cast into the view, regardless of camera visibility (so off-screen
    // occluders still cast). A 16^3 section is a box of half-extent 8 on each world axis, which the
    // volume folds onto its own axes itself.
    private void collectShadowSections(Minecraft mc, ShadowCasterVolume volume, Vec3 camPos) {
        shadowSections.clear();
        ViewArea viewArea = mc.levelRenderer.viewArea;
        if (viewArea == null) return;

        for (SectionRenderDispatcher.RenderSection section : viewArea.sections) {
            SectionRenderDispatcher.CompiledSection compiled = section.getCompiled();
            if (compiled == SectionRenderDispatcher.CompiledSection.UNCOMPILED
                    || compiled.hasNoRenderableLayers()) continue;

            BlockPos origin = section.getOrigin();
            if (castsIntoView(volume, camPos, origin.getX() + 8, origin.getY() + 8, origin.getZ() + 8, 8f)) {
                shadowSections.add(section);
            }
        }
    }

    // Cube test in the volume's camera-relative space; every caller here has a world-space centre and a
    // single conservative half-extent.
    private static boolean castsIntoView(ShadowCasterVolume volume, Vec3 camPos,
                                         double x, double y, double z, float halfExtent) {
        return volume.intersects((float) (x - camPos.x), (float) (y - camPos.y), (float) (z - camPos.z),
                halfExtent, halfExtent, halfExtent);
    }

    // One terrain layer of the collected sections with the light matrices - the body of vanilla's
    // renderSectionLayer minus translucency sorting, profiler and mod render-stage hooks. depthShader is
    // the depth-only program for this layer, or null to fall back to whatever setupRenderState bound.
    private void drawLayer(Minecraft mc, RenderType renderType, Vec3 camPos, ShaderInstance depthShader) {
        renderType.setupRenderState();
        ShaderInstance shader = depthShader != null ? depthShader : RenderSystem.getShader();
        if (shader == null) { // no bound program (another mod cleared it?) - nothing we can draw
            renderType.clearRenderState();
            return;
        }
        try {
            if (depthShader != null) {
                // apply() binds samplers from the shader's own map, not from the RenderSystem globals, so
                // the atlas setupRenderState just bound has to be handed over explicitly. The cutout
                // program needs it for its alpha test; the opaque one declares no sampler and ignores this.
                depthShader.setSampler("Sampler0", RenderSystem.getShaderTexture(0));
            }
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

    // The depth-only terrain programs, loaded on first use. A shadow map only needs depth, so these drop
    // everything the vanilla terrain shaders do for colour - lightmap, overlay, fog, tint, and for the
    // opaque layer the atlas fetch as well, which lets the hardware keep early depth testing on.
    // Null (failed flag set) falls back to the vanilla program, which still renders a correct map.
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
        // Recreate when the configured resolution changes (a shadow_map.json reload can move it).
        int shadowRes = settings.resolution();
        if (shadowTarget == null || shadowTarget.width != shadowRes) {
            if (shadowTarget != null) shadowTarget.destroyBuffers();
            shadowTarget = new TextureTarget(shadowRes, shadowRes, true, Minecraft.ON_OSX);

            // Detach the colour attachment from the draw buffer set. TextureTarget always allocates one
            // and there is no constructor that won't, but nothing has to feed it: with GL_NONE bound the
            // pass writes depth only, which drops the per-frame colour clear and the colour blend/write
            // traffic for every shadow fragment. Framebuffer state, so this sticks for the target's life.
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
        hasRendered = false;
        renderedLevel = null;
        shadowSections.clear();
    }
}
