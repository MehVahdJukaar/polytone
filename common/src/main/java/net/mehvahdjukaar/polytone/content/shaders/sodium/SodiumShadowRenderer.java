package net.mehvahdjukaar.polytone.content.shaders.sodium;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
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

import java.util.List;

// Sodium half of the shadow pass, kept apart so its types only load with Sodium present. See research/POST_SHADOW_NOTES.md.
public final class SodiumShadowRenderer {

    private static GpuTextureView activeShadowColor = null;
    private static GpuTextureView activeShadowDepth = null;

    private static GpuSampler terrainSampler = null;

    public static void captureTerrainSampler(GpuSampler sampler) {
        terrainSampler = sampler;
    }

    public static GpuTextureView activeShadowColorView() {
        return activeShadowColor;
    }

    public static GpuTextureView activeShadowDepthView() {
        return activeShadowDepth;
    }

    public static void replayTerrain(Minecraft mc, Camera cam, Vec3 camPos,
                                     Matrix4f lightView, Matrix4f lightProj,
                                     ShadowCasterVolume volume,
                                     GpuTextureView color, GpuTextureView depth,
                                     List<BlockEntity> blockEntitiesOut) {
        SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instanceNullable();
        if (worldRenderer == null) return;

        RenderSectionManager sectionManager = renderSectionManager();
        boolean mutatedRenderLists = sectionManager != null;
        try {
            if (mutatedRenderLists) cullTerrainToLightVolume(sectionManager, cam, volume, camPos);

            worldRenderer.iterateVisibleBlockEntities(blockEntitiesOut::add);

            GpuSampler sampler = terrainSampler;
            if (sampler == null) return;

            // camera face culling drops the occluding faces from the light POV, draw every face
            var performance = SodiumClientMod.options().performance;
            boolean prevFaceCulling = performance.useBlockFaceCulling;
            performance.useBlockFaceCulling = false;
            activeShadowColor = color;
            activeShadowDepth = depth;
            UniformBufferManager uniforms = ((SodiumWorldRendererShadowAccessor) worldRenderer).polytone$getUniformBufferManager();
            if (uniforms != null) uniforms.prepareFrame();
            try {
                ChunkRenderMatrices matrices = new ChunkRenderMatrices(lightProj, lightView);
                worldRenderer.drawChunkLayer(ChunkSectionLayerGroup.OPAQUE, matrices,
                        camPos.x, camPos.y, camPos.z, sampler);
            } finally {
                if (uniforms != null) uniforms.prepareFrame();
                performance.useBlockFaceCulling = prevFaceCulling;
                activeShadowColor = null;
                activeShadowDepth = null;
            }
        } finally {
            if (mutatedRenderLists) {
                rebuildCameraRenderList(mc, cam);
            }
        }
    }

    private static void cullTerrainToLightVolume(RenderSectionManager sectionManager, Camera camera,
                                                 ShadowCasterVolume volume, Vec3 camPos) {
        SodiumLightVolumeFrustum frustum = new SodiumLightVolumeFrustum(
                volume, Viewport.CHUNK_SECTION_PADDED_RADIUS);
        Viewport viewport = new Viewport(frustum, new Vector3d(camPos.x, camPos.y, camPos.z));

        // frame++, else this traversal appends to the camera's list
        sectionManager.prepareRender();

        sectionManager.finalizeRenderLists(camera, viewport, FogParameters.NONE, true);
    }

    private static void rebuildCameraRenderList(Minecraft mc, Camera camera) {
        RenderSectionManager sectionManager = renderSectionManager();
        if (sectionManager == null) return;
        sectionManager.markGraphDirty();
        Viewport viewport = ((ViewportProvider) camera.getCullFrustum()).sodium$createViewport();
        FogParameters fog = ((FogStorage) mc.gameRenderer).sodium$getFogParameters();
        sectionManager.prepareRender();

        ((SodiumRenderSectionManagerAccessor) sectionManager).polytone$readRenderListFromTree(viewport, fog);
    }

    private static RenderSectionManager renderSectionManager() {
        SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.instanceNullable();
        if (worldRenderer == null) return null;
        return ((SodiumWorldRendererShadowAccessor) worldRenderer).polytone$getRenderSectionManager();
    }
}
