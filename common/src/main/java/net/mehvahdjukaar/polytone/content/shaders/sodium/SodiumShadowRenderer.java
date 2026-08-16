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

public final class SodiumShadowRenderer {

    // MUST only be called with Sodium present: the verifier eager-loads the Sodium types this class
    // references, so the CompatHandler.SODIUM gate has to live at the call site.
    public static void replayTerrain(Minecraft mc, Camera cam, Vec3 camPos,
                                     Matrix4f lightView, Matrix4f lightProj,
                                     ShadowCasterVolume volume) {
        boolean culledToLightVolume = CompatHandler.SODIUM && cullTerrainToLightVolume(mc, cam, camPos, volume);

        SodiumOptions.PerformanceSettings performance = SodiumClientMod.options().performance;
        boolean blockFaceCulling = performance.useBlockFaceCulling;
        performance.useBlockFaceCulling = false;
        try {
            LevelRenderer levelRenderer = mc.levelRenderer;
            levelRenderer.renderSectionLayer(RenderType.solid(), camPos.x, camPos.y, camPos.z, lightView, lightProj);
            levelRenderer.renderSectionLayer(RenderType.cutoutMipped(), camPos.x, camPos.y, camPos.z, lightView, lightProj);
            levelRenderer.renderSectionLayer(RenderType.cutout(), camPos.x, camPos.y, camPos.z, lightView, lightProj);
        } finally {
            performance.useBlockFaceCulling = blockFaceCulling;
            if (culledToLightVolume) markCameraListDirty();
        }
    }

    // false when Sodium's renderer isn't up yet, in which case there's no camera list to restore
    private static boolean cullTerrainToLightVolume(Minecraft mc, Camera camera, Vec3 camPos, ShadowCasterVolume volume) {
        RenderSectionManager sectionManager = renderSectionManager();
        if (sectionManager == null) return false;

        SodiumLightVolumeFrustum frustum = new SodiumLightVolumeFrustum(
                volume, Viewport.CHUNK_SECTION_PADDED_RADIUS);
        Viewport viewport = new Viewport(frustum, new Vector3d(camPos.x, camPos.y, camPos.z));

        // a caster need not be visible to the camera, only inside the light volume.
        // shouldUseOcclusionCulling() keys off smartCull
        boolean smartCull = mc.smartCull;
        mc.smartCull = false;
        try {
            sectionManager.update(camera, viewport, false);
            sectionManager.finalizeRenderLists(viewport);
        } finally {
            mc.smartCull = smartCull;
        }
        return true;
    }

    private static void markCameraListDirty() {
        RenderSectionManager sectionManager = renderSectionManager();
        if (sectionManager != null) sectionManager.markGraphDirty();
    }

    private static RenderSectionManager renderSectionManager() {
        SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instanceNullable();
        if (worldRenderer == null) return null;
        return ((SodiumWorldRendererShadowAccessor) worldRenderer).polytone$getRenderSectionManager();
    }
}
