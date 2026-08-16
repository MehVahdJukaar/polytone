package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.particle.PreviewRenderTarget;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GameRenderer.class, priority = 500)
public abstract class GameRendererMixin {

    @Shadow
    @Final
    private CrossFrameResourcePool resourcePool;

    //TODO: add back
    /*
    @Inject(method = "render", at = @At(value = "NEW",
            target = "Excraft/client/renderer/state/gui/GuiRenderState;II)Lnet/minecraft/client/gui/GuiGraphicsExtractor;"))
    private void polytone$messWithGui(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        Polytone.LIGHTMAPS.setupForGUI(true);
        GuiGraphicsExtractor
        Polytone.OVERLAY_MODIFIERS.onStartRenderingOverlay();
    }*/

    @Inject(method = "render", at = @At(value = "TAIL"))
    private void polytone$resetGuiLightmap(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        Polytone.LIGHTMAPS.setupForGUI(false);
        Polytone.OVERLAY_MODIFIERS.onEndRenderingOverlay();
    }

    // While the particle editor preview draws offscreen, the vanilla particle feature renderer builds
    // its render pass from this target directly - send it to the preview's offscreen buffer instead of
    // the screen. Set only on the render thread for the duration of that one draw, so gameplay is untouched.
    // 26.2: this used to be Minecraft.getMainRenderTarget(); the target now lives on GameRenderer.
    @Inject(method = "mainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void polytone$redirectMainTargetForPreview(CallbackInfoReturnable<RenderTarget> cir) {
        if (!CompatHandler.PACK_EDITOR) return; // the preview that sets this only exists with the editor
        RenderTarget preview = PreviewRenderTarget.current();
        if (preview != null) cir.setReturnValue(preview);
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
