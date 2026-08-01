package net.mehvahdjukaar.polytone.content.shaders.sodium;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.shaders.ShadowCasterVolume;
import net.mehvahdjukaar.polytone.mixins.accessor.SodiumWorldRendererShadowAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;

// All Sodium-specific handling for the shadow pass, kept out of ShadowMapManager so the Sodium types
// load only when Sodium is present (every entry point short-circuits on CompatHandler.SODIUM).
//
// Sodium @Overwrites renderSectionLayer and forwards our matrices to its own drawChunkLayer, so invoking
// it replays Sodium's terrain from the light's POV. But Sodium builds one terrain list per frame culled
// to the camera frustum, which would drop any occluder off screen; we first re-cull that list against the
// light volume (occlusion off, every lit section kept), then dirty the graph so the camera list is rebuilt
// for next frame's main draw.
public final class SodiumShadowRenderer {

    // Replay the opaque terrain layers from the light's POV. MUST only be called when Sodium is present:
    // reCull() references Sodium types (SodiumLightVolumeFrustum -> Frustum), and the verifier eager-loads
    // them when this class is linked, so a call without Sodium on the classpath throws ClassNotFoundException
    // regardless of the CompatHandler.SODIUM runtime guard below. The gate lives at the call site.
    public static void replayTerrain(Minecraft mc, Camera cam, Vec3 camPos,
                                     Matrix4f lightView, Matrix4f lightProj,
                                     ShadowCasterVolume volume) {
        boolean reCulled = CompatHandler.SODIUM && reCull(mc, cam, camPos, volume);

        // Sodium drops the faces of every section that point away from the CAMERA
        // (DefaultChunkRenderer.fillCommandBuffer -> getVisibleFaces, off the CameraTransform built from
        // the position we pass, which has to stay the camera's for ChunkOffset to line up). From the
        // light's side those are exactly the faces that occlude, so leaving it on writes the far side of
        // every block into the map: shadows detach by about a block and walls leak light in bands that
        // shift as you look around. Iris disables the same flag for its shadow pass.
        //
        // The flag is read per fill, and the fill happens because our re-cull above changed the section
        // set, which is what clears the region's cached draw batch (ChunkRenderList -> clearAllCachedBatches).
        // The camera list rebuild next frame changes it back and refills with culling on, so the main pass
        // is unaffected. Costs roughly double the submitted quads here - the same trade Iris makes.
        SodiumOptions.PerformanceSettings performance = SodiumClientMod.options().performance;
        boolean blockFaceCulling = performance.useBlockFaceCulling;
        performance.useBlockFaceCulling = false;
        try {
            LevelRenderer lr = mc.levelRenderer;
            lr.renderSectionLayer(RenderType.solid(), camPos.x, camPos.y, camPos.z, lightView, lightProj);
            lr.renderSectionLayer(RenderType.cutoutMipped(), camPos.x, camPos.y, camPos.z, lightView, lightProj);
            lr.renderSectionLayer(RenderType.cutout(), camPos.x, camPos.y, camPos.z, lightView, lightProj);
        } finally {
            performance.useBlockFaceCulling = blockFaceCulling;
            if (reCulled) restoreCameraList();
        }
    }

    // Re-cull Sodium's terrain list against the light volume. Returns false (nothing done) if Sodium's
    // renderer isn't up yet, in which case there's no camera list to restore.
    private static boolean reCull(Minecraft mc, Camera camera, Vec3 camPos, ShadowCasterVolume volume) {
        RenderSectionManager rsm = renderSectionManager();
        if (rsm == null) return false;

        SodiumLightVolumeFrustum frustum = new SodiumLightVolumeFrustum(
                volume, Viewport.CHUNK_SECTION_PADDED_RADIUS);
        Viewport viewport = new Viewport(frustum, new Vector3d(camPos.x, camPos.y, camPos.z));

        // Force occlusion culling off for this pass: a shadow caster need not be visible to the
        // camera, only inside the light volume. shouldUseOcclusionCulling() keys off smartCull.
        boolean smartCull = mc.smartCull;
        mc.smartCull = false;
        try {
            rsm.update(camera, viewport, false);
            rsm.finalizeRenderLists(viewport);
        } finally {
            mc.smartCull = smartCull;
        }
        return true;
    }

    // Force Sodium to rebuild the camera render list next frame (before the main terrain draw).
    //
    // Deferring the rebuild is only safe because the shadow pass runs at renderLevel TAIL, i.e. after
    // Sodium's own setupTerrain AND after the main terrain draw, so nothing else reads the list we just
    // overwrote this frame. If the hook ever moves earlier, or the version in use moves culling out of
    // renderLevel, this has to rebuild the camera list synchronously instead - on the 26.1 line, where
    // culling moved to LevelRenderer.update, exactly that left the main pass drawing the light-culled
    // set and far terrain blinked out.
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
