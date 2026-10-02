package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.particle.PreviewRenderTarget;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GameRenderer.class, priority = 500)
public abstract class GameRendererMixin {

    @Shadow
    @Final
    private CrossFrameResourcePool resourcePool;

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
    private void polytone$setupGuiLightmap(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        Polytone.LIGHTMAPS.setupForGUI(true);
    }

    @Inject(method = "render", at = @At(value = "TAIL"))
    private void polytone$resetGuiLightmap(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        Polytone.LIGHTMAPS.setupForGUI(false);
    }

    @Inject(method = "mainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void polytone$redirectMainTargetForPreview(CallbackInfoReturnable<RenderTarget> cir) {
        if (!CompatHandler.NAUTILUS) return;
        RenderTarget preview = PreviewRenderTarget.current();
        if (preview != null) cir.setReturnValue(preview);
    }

    // the projection the world is actually rasterised with, view bob and nausea included
    @ModifyArg(method = "renderLevel", index = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    private Matrix4f polytone$captureRenderedProjection(Matrix4f projectionMatrix) {
        Polytone.POST_CHAINS.captureRenderedProjection(projectionMatrix);
        return projectionMatrix;
    }

    @Inject(method = "close", at = @At(value = "TAIL"))
    private void polytone$closeShaderStuff(CallbackInfo ci) {
        Polytone.POST_CHAINS.onClose();
        Polytone.SHADER_EFFECTS.onClose();
        Polytone.SHADOWS.renderer().close();
    }

    // post_chains_after_hand: save the world depth right before vanilla clears it to draw the hand...
    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearDepthTexture(Lcom/mojang/blaze3d/textures/GpuTexture;D)V"))
    private void polytone$snapshotWorldDepth(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!Polytone.CONFIGS.postChainsAfterHand.get()) return;
        Polytone.POST_CHAINS.snapshotWorldDepth(Minecraft.getInstance().gameRenderer.mainRenderTarget());
    }

    // ...then run the chains after the hand, so held items occlude depth effects like godrays
    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V"))
    private void polytone$runPostChainsAfterHand(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        if (!Polytone.CONFIGS.postChainsAfterHand.get()) return;
        Polytone.POST_CHAINS.runChainsAfterHand(Minecraft.getInstance().gameRenderer.mainRenderTarget(), this.resourcePool);
    }

}
