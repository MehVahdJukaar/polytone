package net.mehvahdjukaar.polytone.content.shaders.sodium;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.mehvahdjukaar.polytone.mixins.accessor.SodiumWorldRendererShadowAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL30;

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
// section kept), then dirty the graph so the camera list is rebuilt for next frame's main draw.
public final class SodiumShadowRenderer {

    // Set for the duration of a shadow terrain replay. Sodium's ShaderChunkRenderer.begin binds the MAIN
    // framebuffer + viewport; while this is active our begin mixin rebinds them to the shadow map so the
    // terrain draws land there. Render-thread only, so a plain static is fine.
    private static boolean active = false;
    private static GpuTexture shadowColor = null;
    private static GpuTexture shadowDepth = null;

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
        int fbo = ((GlTexture) shadowColor).getFbo(
                ((GlDevice) RenderSystem.getDevice()).directStateAccess(), shadowDepth);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GlStateManager._viewport(0, 0, shadowColor.getWidth(0), shadowColor.getHeight(0));
    }

    // Replay Sodium's opaque terrain (SOLID + CUTOUT) from the light's POV into the shadow attachments.
    // MUST only be called with Sodium present: it references Sodium types the verifier eager-loads when
    // this class is linked, so the CompatHandler.SODIUM gate has to live at the call site (ShadowMapRenderer).
    public static void replayTerrain(Minecraft mc, Camera cam, Vec3 camPos,
                                     Matrix4f lightView, Matrix4f lightProj,
                                     float coverage, float depthRange,
                                     GpuTexture color, GpuTexture depth) {
        SodiumWorldRenderer swr = SodiumWorldRenderer.instanceNullable();
        GpuSampler sampler = terrainSampler;
        if (swr == null || sampler == null) return; // renderer not up, or no sampler captured yet

        boolean reCulled = reCull(mc, cam, lightView, camPos, coverage, depthRange);
        active = true;
        shadowColor = color;
        shadowDepth = depth;
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
            active = false;
            shadowColor = null;
            shadowDepth = null;
            if (reCulled) restoreCameraList();
        }
    }

    // Re-cull Sodium's terrain list against the light volume. Returns false (nothing done) if Sodium's
    // renderer isn't up yet, in which case there's no camera list to restore.
    private static boolean reCull(Minecraft mc, Camera camera, Matrix4f lightView, Vec3 camPos,
                                  float coverage, float depthRange) {
        RenderSectionManager rsm = renderSectionManager();
        if (rsm == null) return false;

        SodiumLightVolumeFrustum frustum = new SodiumLightVolumeFrustum(
                lightView, coverage, depthRange, Viewport.CHUNK_SECTION_PADDED_RADIUS);
        Viewport viewport = new Viewport(frustum, new Vector3d(camPos.x, camPos.y, camPos.z));

        // Force occlusion culling off for this pass: a shadow caster need not be visible to the camera,
        // only inside the light volume. shouldUseOcclusionCulling() keys off smartCull. FogParameters.NONE
        // so fog distance never trims occluders inside the coverage box.
        boolean smartCull = mc.smartCull;
        mc.smartCull = false;
        try {
            rsm.update(camera, viewport, FogParameters.NONE, false);
            rsm.finalizeRenderLists(viewport);
        } finally {
            mc.smartCull = smartCull;
        }
        return true;
    }

    // Force Sodium to rebuild the camera render list next frame (before the main terrain draw).
    private static void restoreCameraList() {
        RenderSectionManager rsm = renderSectionManager();
        if (rsm != null) rsm.markGraphDirty();
    }

    private static RenderSectionManager renderSectionManager() {
        SodiumWorldRenderer swr = SodiumWorldRenderer.instanceNullable();
        if (swr == null) return null;
        return ((SodiumWorldRendererShadowAccessor) swr).polytone$getRenderSectionManager();
    }
}