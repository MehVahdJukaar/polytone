package net.mehvahdjukaar.polytone.content.shaders.sodium;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.shaders.ShadowCasterVolume;
import net.mehvahdjukaar.polytone.mixins.accessor.LevelRendererShadowAccessor;
import net.mehvahdjukaar.polytone.mixins.accessor.SodiumWorldRendererShadowAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
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

        LevelRendererShadowAccessor lr = (LevelRendererShadowAccessor) mc.levelRenderer;
        //TODO: use access wideners instead of the @Invoker
        lr.polytone$renderSectionLayer(RenderType.solid(), camPos.x, camPos.y, camPos.z, lightView, lightProj);
        lr.polytone$renderSectionLayer(RenderType.cutoutMipped(), camPos.x, camPos.y, camPos.z, lightView, lightProj);
        lr.polytone$renderSectionLayer(RenderType.cutout(), camPos.x, camPos.y, camPos.z, lightView, lightProj);

        if (reCulled) restoreCameraList();
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
