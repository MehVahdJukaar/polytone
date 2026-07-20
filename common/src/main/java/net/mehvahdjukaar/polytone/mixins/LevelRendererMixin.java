package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.particle.PreviewRenderTarget;
import net.mehvahdjukaar.polytone.content.particle.custom.ParticleLightCache;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    public LevelRenderState levelRenderState;

    // During the particle editor preview the main target is redirected to the offscreen buffer; force
    // the separate translucent-particles target to null too, so opaque AND translucent particles render
    // in the single main-target pass (into the offscreen buffer) instead of a screen-bound target.
    @Inject(method = "getParticlesTarget", at = @At("HEAD"), cancellable = true)
    private void poly$redirectParticlesTargetForPreview(CallbackInfoReturnable<RenderTarget> cir) {
        if (CompatHandler.PACK_EDITOR && PreviewRenderTarget.current() != null) cir.setReturnValue(null);
    }

    // Every section rebuild (block or light change) funnels through setSectionDirty; bump that
    // section's light-cache version so particles there re-sample. Section coords come in directly.
    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
    private void poly$invalidateParticleLight(int x, int y, int z, boolean important, CallbackInfo ci) {
        ParticleLightCache.markSectionDirty(x, y, z);
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    public void poly$preRender(GraphicsResourceAllocator graphicsResourceAllocator,
                          DeltaTracker deltaTracker,
                          boolean bl,
                          Camera camera,
                          Matrix4f modelView,
                          Matrix4f project,
                          Matrix4f matrix4f3,
                          GpuBufferSlice gpuBufferSlice,
                          Vector4f vector4f,
                          boolean bl2, CallbackInfo ci) {
        // deltaTime from this call's own tracker parameter, fresh every render call
        Polytone.POST_CHAINS.captureLevelRendererParams(project, modelView, deltaTracker.getGameTimeDeltaTicks());
        // upload expression-driven UBOs now, while no render pass is open; tryApply() only binds them
        Polytone.SHADER_EFFECTS.updateAll();
        // Render the directional shadow map here: no render pass is open (UBO writes need that), last
        // frame's compiled section meshes are still current, and the frame graph that runs the post
        // chains sampling it hasn't been built yet.
        Polytone.SHADOWS.renderer().renderShadowPassIfNeeded(gpuBufferSlice);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addLateDebugPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/state/CameraRenderState;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4f;)V",
    shift = At.Shift.BEFORE))
    public void poly$addPostShaders(GraphicsResourceAllocator graphicsResourceAllocator,
                                    DeltaTracker deltaTracker,
                                    boolean bl,
                                    Camera camera,
                                    Matrix4f modelView,
                                    Matrix4f project,
                                    Matrix4f matrix4f3,
                                    GpuBufferSlice gpuBufferSlice,
                                    Vector4f vector4f,
                                    boolean bl2, CallbackInfo ci,
                                    @Local FrameGraphBuilder frameGraphBuilder) {
        int i = this.minecraft.getMainRenderTarget().width;
        int j = this.minecraft.getMainRenderTarget().height;
        Polytone.POST_CHAINS.addPostPass(i, j, this.targets, frameGraphBuilder, gpuBufferSlice, this.levelRenderState.cameraRenderState);
    }
}
