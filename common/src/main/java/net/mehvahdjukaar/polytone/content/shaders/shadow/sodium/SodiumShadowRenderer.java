package net.mehvahdjukaar.polytone.content.shaders.shadow.sodium;

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
import net.caffeinemc.mods.sodium.client.render.chunk.lists.DeferredTaskList;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.render.viewport.ViewportProvider;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.caffeinemc.mods.sodium.client.util.FogStorage;
import net.mehvahdjukaar.polytone.content.shaders.shadow.ShadowCasterVolume;
import net.mehvahdjukaar.polytone.mixins.compat.SodiumRenderSectionManagerAccessor;
import net.mehvahdjukaar.polytone.mixins.compat.SodiumWorldRendererShadowAccessor;
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

//sodium binds the main framebuffer itself in ShaderChunkRenderer.begin, the begin mixin repoints it to ours
public final class SodiumShadowRenderer {

    private static boolean active = false;
    private static GpuTexture shadowColor = null;
    private static GpuTexture shadowDepth = null;

    private static boolean savedBinding = false;
    private static int prevFbo = 0;
    private static final int[] prevViewport = new int[4];

    //null until the first main terrain draw, shadow terrain shows up a frame later
    private static GpuSampler terrainSampler = null;

    public static void captureTerrainSampler(GpuSampler sampler) {
        terrainSampler = sampler;
    }

    //no sodium types in here, the mixin calls it unconditionally
    public static void rebindShadowFramebufferIfActive() {
        if (!active || shadowColor == null || shadowDepth == null) return;
        if (!savedBinding) {
            prevFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevViewport);
            savedBinding = true;
        }
        GlDevice glDevice = (GlDevice) RenderSystem.getDevice().backend;
        int fbo = ((GlTexture) shadowColor).getFbo(glDevice.directStateAccess(), shadowDepth);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GlStateManager._viewport(0, 0, shadowColor.getWidth(0), shadowColor.getHeight(0));
    }

    private static void restoreFramebufferBinding() {
        if (!savedBinding) return;
        savedBinding = false;
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
        GlStateManager._viewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
    }

    public static void replayTerrain(Minecraft mc, Camera cam, Vec3 camPos,
                                     Matrix4f lightView, Matrix4f lightProj,
                                     ShadowCasterVolume volume,
                                     GpuTexture color, GpuTexture depth,
                                     List<BlockEntity> blockEntitiesOut) {
        SodiumWorldRenderer swr = SodiumWorldRenderer.instanceNullable();
        if (swr == null) return;

        RenderSectionManager rsm = renderSectionManager();
        boolean mutatedLists = rsm != null;
        SodiumRenderSectionManagerAccessor sodiumAcc = (SodiumRenderSectionManagerAccessor) rsm;
        DeferredTaskList oldCameraTasks = mutatedLists
                ? sodiumAcc.polytone$getTaskLists() : null;
        try {
            if (mutatedLists) {
                cullTerrainToLightVolume(rsm, cam, volume, camPos);
            }

            swr.iterateVisibleBlockEntities(blockEntitiesOut::add);

            GpuSampler sampler = terrainSampler;
            if (sampler == null) return;

            //sodium culls faces facing away from the camera, which are the ones that occlude from the light
            var performance = SodiumClientMod.options().performance;
            boolean prevFaceCulling = performance.useBlockFaceCulling;
            performance.useBlockFaceCulling = false;
            active = true;
            shadowColor = color;
            shadowDepth = depth;
            UniformBufferManager uniforms = ((SodiumWorldRendererShadowAccessor) swr).polytone$getUniformBufferManager();
            if (uniforms != null) uniforms.prepareFrame();
            try {
                ChunkRenderMatrices matrices = new ChunkRenderMatrices(lightProj, lightView);
                //sodium's own renderGroup hook wraps this in managed code too, its gl state tracking asserts on it
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
            if (mutatedLists) {
                sodiumAcc.polytone$setTaskLists(oldCameraTasks);
                restoreCameraList(mc, cam);
            }
        }
    }

    private static void cullTerrainToLightVolume(RenderSectionManager rsm, Camera camera,
                                                 ShadowCasterVolume volume, Vec3 camPos) {
        SodiumShadowCasterFrustum frustum = new SodiumShadowCasterFrustum(volume, Viewport.CHUNK_SECTION_PADDED_RADIUS);
        Viewport viewport = new Viewport(frustum, new Vector3d(camPos.x, camPos.y, camPos.z));

        //new frame number, a second traversal in the same frame appends to the camera lists ("Render list is full")
        rsm.prepareRender();
        //sync path, frustum only. the occlusion tree is built off thread for the camera
        rsm.finalizeRenderLists(camera, viewport, FogParameters.NONE, true);
    }

    private static void restoreCameraList(Minecraft mc, Camera camera) {
        RenderSectionManager rsm = renderSectionManager();
        if (rsm == null) return;
        try {
            Viewport viewport = ((ViewportProvider) camera.getCullFrustum()).sodium$createViewport();
            FogParameters fog = ((FogStorage) mc.gameRenderer).sodium$getFogParameters();
            rsm.prepareRender();

            ((SodiumRenderSectionManagerAccessor) rsm).polytone$readRenderListFromTree(viewport, fog);
        } catch (RuntimeException e) {
            rsm.markGraphDirty();
            throw e;
        }
    }

    private static RenderSectionManager renderSectionManager() {
        SodiumWorldRenderer swr = SodiumWorldRenderer.instanceNullable();
        if (swr == null) return null;
        return ((SodiumWorldRendererShadowAccessor) swr).polytone$getRenderSectionManager();
    }
}
