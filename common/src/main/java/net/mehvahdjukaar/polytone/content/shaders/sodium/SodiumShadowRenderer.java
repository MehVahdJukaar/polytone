package net.mehvahdjukaar.polytone.content.shaders.sodium;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.render.viewport.ViewportProvider;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.caffeinemc.mods.sodium.client.util.FogStorage;
import net.mehvahdjukaar.polytone.content.shaders.ShadowCasterVolume;
import net.mehvahdjukaar.polytone.mixins.accessor.SodiumRenderSectionManagerAccessor;
import net.mehvahdjukaar.polytone.mixins.accessor.SodiumWorldRendererShadowAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.List;

// All Sodium-specific handling for the shadow pass, kept out of ShadowMapRenderer so the Sodium types
// load only when Sodium is present (the replay entry is called behind CompatHandler.SODIUM, and the
// capture/rebind entries only from the @Pseudo SodiumChunkRendererMixin, which never applies without it).
//
// On 1.21.11 Sodium renders terrain through its own GL path, not vanilla's ChunkSectionsToRender: it
// cancels renderGroup and draws in RenderSectionManager.renderLayer, binding the framebuffer itself from
// TerrainRenderPass.getTarget() (always the MAIN target for opaque). So we can't just hand it the light
// matrices like on 1.21.1 - we drive SodiumWorldRenderer.drawChunkLayer with the light matrices AND
// repoint its framebuffer at the shadow attachments (rebindShadowFramebufferIfActive, from the begin
// mixin). Sodium also builds one terrain list per frame culled to the camera frustum, which would drop
// any occluder off screen; we first re-cull that list against the light volume (occlusion off, every lit
// section kept), then rebuild the camera list before returning - the main draw happens later in this same
// frame and reads that list live (see restoreCameraList).
public final class SodiumShadowRenderer {

    // Set for the duration of a shadow terrain replay. Sodium's ShaderChunkRenderer.begin binds the MAIN
    // framebuffer + viewport; while this is active our begin mixin rebinds them to the shadow map so the
    // terrain draws land there. Render-thread only, so a plain static is fine.
    private static boolean active = false;
    private static GpuTexture shadowColor = null;
    private static GpuTexture shadowDepth = null;

    // GL framebuffer + viewport as they were before the first rebind of a replay, so the replay can put
    // them back. Both vanilla (RenderPass) and Sodium (begin) bind their own target before drawing, so
    // this is belt-and-braces - but we mutate raw GL state behind their backs, and anything that draws
    // in between assuming the main target would otherwise land in the shadow map at shadow resolution.
    private static boolean savedBinding = false;
    private static int prevFbo = 0;
    private static final int[] prevViewport = new int[4];

    // The block-atlas GpuSampler vanilla hands to the terrain draw. Captured from the main pass and fed
    // back into drawChunkLayer for the replay. Effectively constant, so last frame's is fine; null until
    // the first main terrain draw of the session (shadow terrain then appears one frame later).
    private static GpuSampler terrainSampler = null;

    // Called from the begin mixin on every terrain draw; keeps the latest atlas sampler for the replay.
    public static void captureTerrainSampler(GpuSampler sampler) {
        terrainSampler = sampler;
    }

    // Called from the begin mixin (TAIL). Sodium has just bound the MAIN framebuffer + viewport; when a
    // shadow replay is in flight, repoint both at the shadow map. Touches only always-present GL classes
    // (no Sodium types), so it's safe to call unconditionally from the mixin.
    public static void rebindShadowFramebufferIfActive() {
        if (!active || shadowColor == null || shadowDepth == null) return;
        if (!savedBinding) {
            prevFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevViewport);
            savedBinding = true;
        }
        // 26.1 wraps the GL device: RenderSystem.getDevice() is the frontend, the GlDevice is its backend.
        GlDevice glDevice = (GlDevice) RenderSystem.getDevice().backend;
        int fbo = ((GlTexture) shadowColor).getFbo(glDevice.directStateAccess(), shadowDepth);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GlStateManager._viewport(0, 0, shadowColor.getWidth(0), shadowColor.getHeight(0));
    }

    // Undo whatever rebindShadowFramebufferIfActive did during the replay. No-op if it never fired.
    private static void restoreFramebufferBinding() {
        if (!savedBinding) return;
        savedBinding = false;
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
        GlStateManager._viewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
    }

    // Replay Sodium's opaque terrain (SOLID + CUTOUT) from the light's POV into the shadow attachments,
    // and collect the block entities of the same light-culled section set into blockEntitiesOut (Sodium
    // owns those too - the vanilla section meshes ShadowMapRenderer scans are empty here).
    // MUST only be called with Sodium present: it references Sodium types the verifier eager-loads when
    // this class is linked, so the CompatHandler.SODIUM gate has to live at the call site (ShadowMapRenderer).
    public static void replayTerrain(Minecraft mc, Camera cam, Vec3 camPos,
                                     Matrix4f lightView, Matrix4f lightProj,
                                     ShadowCasterVolume volume,
                                     GpuTexture color, GpuTexture depth,
                                     List<BlockEntity> blockEntitiesOut) {
        SodiumWorldRenderer swr = SodiumWorldRenderer.instanceNullable();
        if (swr == null) return; // renderer not up

        // Claim the restore BEFORE touching the render lists: reCull mutates Sodium's shared state, so if
        // anything from here on throws, the light-culled list must still be thrown away. Leaving it in
        // place would make this frame's MAIN terrain draw use the sun's culling - chunks missing behind
        // the camera, the classic "the world is being culled from the sun" symptom.
        RenderSectionManager rsm = renderSectionManager();
        boolean mutatedLists = rsm != null;
        try {
            if (mutatedLists) reCull(rsm, cam, volume, camPos);

            // The render lists now hold the light-volume section set, so this yields exactly the block
            // entities that can cast into the map (plus the global ones, which are never culled).
            swr.iterateVisibleBlockEntities(blockEntitiesOut::add);

            GpuSampler sampler = terrainSampler;
            if (sampler == null) return; // no atlas sampler captured yet: terrain draws from next frame on

            // Sodium culls each section's faces to those facing the CAMERA (getVisibleFaces keys off the
            // CameraTransform we pass = the real camera). From the light's POV that drops the faces that
            // actually occlude, so the depth map gets holes (light-leak bands that shift as the camera moves)
            // and often records the wrong face as nearest (a ~1 block shadow offset). Draw every face for the
            // shadow pass. Restored right after, before the main terrain draw later this frame.
            var performance = SodiumClientMod.options().performance;
            boolean prevFaceCulling = performance.useBlockFaceCulling;
            performance.useBlockFaceCulling = false;
            active = true;
            shadowColor = color;
            shadowDepth = depth;
            // Since 0.9 the terrain shader reads its matrices from a UBO that UniformBufferManager writes
            // ONCE PER FRAME - update() no-ops after the first call until prepareFrame() clears the flag.
            // Our replay runs at renderLevel HEAD, i.e. BEFORE the main terrain draw, so without this the
            // light matrices would be the frame's one write and the MAIN pass would silently reuse them:
            // the whole world drawn from the sun's POV on every shadow frame. Clear it on the way in (so
            // our light matrices are really what gets written) and again on the way out (so the main pass
            // writes its own camera matrices back).
            UniformBufferManager uniforms = ((SodiumWorldRendererShadowAccessor) swr).polytone$getUniformBufferManager();
            if (uniforms != null) uniforms.prepareFrame();
            try {
                ChunkRenderMatrices matrices = new ChunkRenderMatrices(lightProj, lightView);
                // Sodium's own renderGroup hook wraps drawChunkLayer in managed code (its GL-state tracking
                // asserts on it); we drive drawChunkLayer directly, so we mirror that here.
                RenderDevice.enterManagedCode();
                try {
                    swr.drawChunkLayer(ChunkSectionLayerGroup.OPAQUE, matrices,
                            camPos.x, camPos.y, camPos.z, sampler);
                } finally {
                    RenderDevice.exitManagedCode();
                }
            } finally {
                if (uniforms != null) uniforms.prepareFrame();
                performance.useBlockFaceCulling = prevFaceCulling;
                active = false;
                shadowColor = null;
                shadowDepth = null;
                restoreFramebufferBinding();
            }
        } finally {
            if (mutatedLists) restoreCameraList(mc, cam);
        }
    }

    // Re-cull Sodium's terrain list against the caster volume.
    private static void reCull(RenderSectionManager rsm, Camera camera,
                               ShadowCasterVolume volume, Vec3 camPos) {
        SodiumLightVolumeFrustum frustum = new SodiumLightVolumeFrustum(
                volume, Viewport.CHUNK_SECTION_PADDED_RADIUS);
        Viewport viewport = new Viewport(frustum, new Vector3d(camPos.x, camPos.y, camPos.z));

        // A region's ChunkRenderList is only cleared when the collector meets it at a NEW frame number
        // (getLastVisibleFrame() != frame), so a second traversal within one frame APPENDS to the camera's
        // list instead of replacing it - mixed lists, then "Render list is full" once a region passes 256.
        // prepareRender() is the frame++ Sodium itself calls before each of its own traversals.
        rsm.prepareRender();

        // updateChunksImmediately = true picks finalizeRenderLists' synchronous fallback traversal: every
        // renderable section inside the viewport, culled by the frustum alone. That's what a shadow pass
        // wants (a caster need not be visible from the camera, only inside the light volume) and it is the
        // only path that doesn't route through Sodium's occlusion trees, which since 0.9 are built
        // off-thread for the CAMERA viewport and can't be rebuilt for ours mid-frame. FogParameters.NONE
        // so fog distance never trims occluders inside the coverage box.
        rsm.finalizeRenderLists(camera, viewport, FogParameters.NONE, true);
    }

    // Put the CAMERA's render list back, right now. Deferring this to Sodium (markGraphDirty and let its
    // own setupTerrain redo it) worked on 1.21.11, where terrain culling ran inside renderLevel after our
    // HEAD hook. On 26.1 culling moved to LevelRenderer.update, which runs BEFORE renderLevel, so a dirty
    // flag only rebuilds NEXT frame - and the main terrain draw reads rsm.renderLists live when the frame
    // graph executes, so it would draw our light-culled list: every section outside the light volume
    // missing, sky where the far terrain should be, blinking at the shadow update interval.
    // Same inputs Sodium's own cullTerrain uses, so the rebuilt list matches what it would have produced.
    private static void restoreCameraList(Minecraft mc, Camera camera) {
        RenderSectionManager rsm = renderSectionManager();
        if (rsm == null) return;
        // First, so that even if the rebuild below throws, Sodium redoes the cull next frame.
        rsm.markGraphDirty();
        Viewport viewport = ((ViewportProvider) camera.getCullFrustum()).sodium$createViewport();
        FogParameters fog = ((FogStorage) mc.gameRenderer).sodium$getFogParameters();
        // New frame number again, for the same reason as in reCull: this traversal has to CLEAR the lists
        // the shadow cull just filled, not append to them.
        rsm.prepareRender();

        // Straight into the tree read, skipping the public entry points: the occlusion trees the async
        // culler produced for THIS camera earlier in the frame are still there and still valid, so this
        // reproduces the list Sodium would have drawn. finalizeRenderLists would instead see the flags our
        // shadow cull just cleared and fall back to a frustum-only list (every section in view, no
        // occlusion culling) for the rest of the frame.
        ((SodiumRenderSectionManagerAccessor) rsm).polytone$readRenderListFromTree(viewport, fog);
    }

    private static RenderSectionManager renderSectionManager() {
        SodiumWorldRenderer swr = SodiumWorldRenderer.instanceNullable();
        if (swr == null) return null;
        return ((SodiumWorldRendererShadowAccessor) swr).polytone$getRenderSectionManager();
    }
}