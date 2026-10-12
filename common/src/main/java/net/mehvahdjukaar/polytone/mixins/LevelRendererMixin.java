package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.shaders.LevelRenderPassTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Shadow
    @Final
    public LevelRenderState levelRenderState;

    @Inject(method = "render", at = @At("HEAD"))
    public void poly$preRender(GraphicsResourceAllocator resourceAllocator,
                               boolean renderOutline,
                               CameraRenderState cameraState,
                               GpuBufferSlice terrainFog,
                               Vector4f fogColor,
                               boolean shouldRenderSky,
                               boolean consistentDepthRequired,
                               CallbackInfo ci) {
        Polytone.SHADER_UNIFORMS.updateAll();
        if (!LevelRenderPassTracker.pushAndIsMain()) return;
        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        DeltaTracker deltaTracker = Minecraft.getInstance().getDeltaTracker();
        Polytone.GLOBAL_UNIFORMS.capture(cameraState.projectionMatrix, cameraState.viewRotationMatrix, deltaTracker.getGameTimeDeltaTicks());
        //Polytone.COLORED_LIGHTS.beforeRenderLevel(camera.position());
    }

    //the frame graph already ran here, no pass is open
    @Inject(method = "render", at = @At("RETURN"))
    public void poly$popLevelRenderPass(CallbackInfo ci) {
        if (!LevelRenderPassTracker.popAndWasMain()) return;
        //Polytone.VOXEL_VOLUME.updateAfterRenderLevel(Minecraft.getInstance().gameRenderer.mainCamera());
    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/LevelRenderer;addMainPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;Z)V"))
    public void poly$addGpuParticlesPass(GraphicsResourceAllocator resourceAllocator,
                                         boolean renderOutline,
                                         CameraRenderState cameraState,
                                         GpuBufferSlice terrainFog,
                                         Vector4f fogColor,
                                         boolean shouldRenderSky,
                                         boolean consistentDepthRequired,
                                         CallbackInfo ci,
                                         @Local FrameGraphBuilder frameGraphBuilder) {
        Polytone.CUSTOM_PARTICLES.gpuParticles.addRenderPass(frameGraphBuilder, this.targets, terrainFog,
                this.levelRenderState.cameraRenderState.pos, this.levelRenderState.gameTime,
                Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;execute(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;)V",
            shift = At.Shift.BEFORE))
    public void poly$addPostShaders(GraphicsResourceAllocator resourceAllocator,
                                    boolean renderOutline,
                                    CameraRenderState cameraState,
                                    GpuBufferSlice terrainFog,
                                    Vector4f fogColor,
                                    boolean shouldRenderSky,
                                    boolean consistentDepthRequired,
                                    CallbackInfo ci,
                                    @Local FrameGraphBuilder frameGraphBuilder) {
        if (Polytone.CONFIGS.postChainsAfterHand.get()) return;
        RenderTarget mainTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        Polytone.POST_CHAINS.addChainsToFrameGraph(mainTarget.width, mainTarget.height, this.targets, frameGraphBuilder,
                terrainFog, this.levelRenderState.cameraRenderState);
    }
}
