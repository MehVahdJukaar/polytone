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

/**
 * Isolated Sodium hook for the shadow pass. Sodium builds a single terrain render list per frame,
 * culled to the CAMERA frustum; replaying it with the light matrices (as {@code ShadowMapManager}
 * does) therefore drops any occluder that isn't on screen, so mountains / tree tops stop casting
 * shadows the moment they leave the view - most visibly when tilting the camera down.
 *
 * <p>This rebuilds that render list against the shadow LIGHT volume instead (occlusion culling off,
 * so every lit section in the volume is included, not just those reachable through the camera's
 * visibility graph), then lets the normal {@code drawChunkLayer} replay it. {@link #finish} marks the
 * graph dirty so Sodium rebuilds the camera list next frame before the main terrain draw.</p>
 *
 * <p>Kept in its own class so the Sodium types load only when Sodium is actually present (callers
 * gate on {@code CompatHandler.SODIUM}).</p>
 */
final class SodiumShadowCuller {

    private SodiumShadowCuller() {}

    /**
     * Re-cull Sodium's render list against the light volume. After this returns true, invoking
     * {@code renderSectionLayer} with the light matrices draws the light-culled terrain. Pair every
     * successful call with {@link #finish}.
     */
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

    /** Force Sodium to rebuild the camera render list next frame (before the main terrain draw). */
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
