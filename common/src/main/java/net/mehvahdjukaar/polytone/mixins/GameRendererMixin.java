package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.mehvahdjukaar.polytone.Polytone;
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

    @Inject(method = "close", at = @At(value = "TAIL"))
    private void polytone$closeShaderStuff(CallbackInfo ci) {
        Polytone.POST_CHAINS.onClose();
        Polytone.SHADER_EFFECTS.onClose();
    }

    // Depth-aware post chains (post_chains_after_hand): save the finished world depth right before
    // vanilla clears it to draw the first-person hand in its own near projection.
    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearDepthTexture(Lcom/mojang/blaze3d/textures/GpuTexture;D)V"))
    private void polytone$snapshotWorldDepth(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!Polytone.CONFIGS.postChainsAfterHand.get()) return;
        Polytone.POST_CHAINS.snapshotWorldDepth(Minecraft.getInstance().getMainRenderTarget());
    }

    // ...then, once the hand has been drawn, fold the world depth back in and run the chains so
    // held items (e.g. a shield) occlude depth effects like godrays.
    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V"))
    private void polytone$runPostChainsAfterHand(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        if (!Polytone.CONFIGS.postChainsAfterHand.get()) return;
        Polytone.POST_CHAINS.runAfterHand(Minecraft.getInstance().getMainRenderTarget(), this.resourcePool);
    }

}
