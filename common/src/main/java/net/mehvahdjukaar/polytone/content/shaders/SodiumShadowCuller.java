package net.mehvahdjukaar.polytone.content.shaders;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.mehvahdjukaar.polytone.mixins.accessor.SodiumWorldRendererShadowAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;

// Isolated Sodium hook for the shadow pass. Sodium builds one terrain render list per frame, culled to
// the camera frustum; replaying it with the light matrices (as ShadowMapManager does) drops any occluder
// that isn't on screen, so mountains and tree tops stop casting shadows once they leave the view. reCull
// rebuilds that list against the light volume instead (occlusion culling off, so every lit section is
// kept), then finish() dirties the graph so Sodium rebuilds the camera list next frame.
// In its own class so the Sodium types load only when Sodium is present (callers gate on CompatHandler.SODIUM).
final class SodiumShadowCuller {

    // Re-cull against the light volume. After this returns true, invoking renderSectionLayer with the
    // light matrices draws the light-culled terrain. Pair every successful call with finish().
    static boolean reCull(Minecraft mc, Camera camera, Matrix4f lightView, Vec3 camPos,
                          float coverage, float depthRange) {
        RenderSectionManager rsm = renderSectionManager();
        if (rsm == null) return false;

        LightVolumeFrustum frustum = new LightVolumeFrustum(
                lightView, coverage, depthRange, Viewport.CHUNK_SECTION_PADDED_RADIUS);
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
    static void finish() {
        RenderSectionManager rsm = renderSectionManager();
        if (rsm != null) rsm.markGraphDirty();
    }

    private static RenderSectionManager renderSectionManager() {
        SodiumWorldRenderer swr = SodiumWorldRenderer.instanceNullable();
        if (swr == null) return null;
        return ((SodiumWorldRendererShadowAccessor) swr).polytone$getRenderSectionManager();
    }
}
