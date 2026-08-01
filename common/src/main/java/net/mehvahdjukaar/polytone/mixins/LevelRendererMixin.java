package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.shaders.LevelRenderPassTrack;
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
        // no render pass is open here, which the UBO writes below need
        Polytone.SHADER_EFFECTS.updateAll();
        //a second camera from some other mod. shadows and post chains only care about the main one
        if (!LevelRenderPassTrack.pushAndIsMain()) return;
        DeltaTracker deltaTracker = Minecraft.getInstance().getDeltaTracker();
        Polytone.POST_CHAINS.updateGlobalUniforms(cameraState.projectionMatrix, cameraState.viewRotationMatrix,
                deltaTracker.getGameTimeDeltaTicks());
        // shadow map goes first so the post chains built into this frame's graph sample this frame's map
        Polytone.SHADOWS.renderer().renderShadowPassIfNeeded(terrainFog, Minecraft.getInstance().gameRenderer.mainCamera(),
                cameraState.viewRotationMatrix, cameraState.projectionMatrix);
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void poly$popLevelRenderPass(CallbackInfo ci) {
        LevelRenderPassTrack.pop();
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
