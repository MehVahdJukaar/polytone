package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.particle.PreviewRenderTarget;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4fc;
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
    private LevelRenderState levelRenderState;

    // During the particle editor preview the main target is redirected to the offscreen buffer; force
    // the separate translucent-particles target to null too, so opaque AND translucent particles render
    // in the single main-target pass (into the offscreen buffer) instead of a screen-bound target.
    // 26.2: getParticlesTarget() -> particlesTarget()
    @Inject(method = "particlesTarget", at = @At("HEAD"), cancellable = true)
    private void poly$redirectParticlesTargetForPreview(CallbackInfoReturnable<RenderTarget> cir) {
        if (CompatHandler.PACK_EDITOR && PreviewRenderTarget.current() != null) cir.setReturnValue(null);
    }

    // 26.2: LevelRenderer.renderLevel(...) -> render(...) (dropped the ChunkSectionsToRender param; it's a local now)
    @Inject(method = "render", at = @At("HEAD"))
    public void poly$preRender(GraphicsResourceAllocator resourceAllocator,
                               DeltaTracker deltaTracker,
                               boolean renderOutline,
                               CameraRenderState cameraState,
                               Matrix4fc modelViewMatrix,
                               GpuBufferSlice terrainFog,
                               Vector4f fogColor,
                               boolean shouldRenderSky,
                               CallbackInfo ci) {
        // deltaTime from this call's own tracker parameter, fresh every render call
        Polytone.POST_CHAINS.captureLevelRendererParams(cameraState.projectionMatrix, modelViewMatrix,
                deltaTracker.getGameTimeDeltaTicks());
        // upload expression-driven UBOs now, while no render pass is open; tryApply() only binds them
        Polytone.SHADER_EFFECTS.updateAll();
        // Render the directional shadow map here: no render pass is open (UBO writes need that), last
        // frame's compiled section meshes are still current, and the frame graph that runs the post
        // chains sampling it hasn't been built yet. The matrices are this call's own rather than the
        // GameRenderer globals, so they still agree when a mod renders a second view; they narrow the
        // caster volume to what the camera can see.
        Polytone.SHADOWS.renderer().renderShadowPassIfNeeded(terrainFog, Minecraft.getInstance().gameRenderer.mainCamera(),
                modelViewMatrix, cameraState.projectionMatrix);
    }

    // 26.2: addLateDebugPass(...) was removed; inject our post passes into the frame graph right before it executes.
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;execute(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;)V",
            shift = At.Shift.BEFORE))
    public void poly$addPostShaders(GraphicsResourceAllocator resourceAllocator,
                                    DeltaTracker deltaTracker,
                                    boolean renderOutline,
                                    CameraRenderState cameraState,
                                    Matrix4fc modelViewMatrix,
                                    GpuBufferSlice terrainFog,
                                    Vector4f fogColor,
                                    boolean shouldRenderSky,
                                    CallbackInfo ci,
                                    @Local FrameGraphBuilder frameGraphBuilder) {
        // Standard placement only. When post_chains_after_hand is on (default), chains run later,
        // after the first-person hand, from GameRendererMixin so held items occlude depth effects.
        if (Polytone.CONFIGS.postChainsAfterHand.get()) return;
        RenderTarget mainTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        int i = mainTarget.width;
        int j = mainTarget.height;
        Polytone.POST_CHAINS.addPostPass(i, j, this.targets, frameGraphBuilder, terrainFog, this.levelRenderState.cameraRenderState);
    }
}
