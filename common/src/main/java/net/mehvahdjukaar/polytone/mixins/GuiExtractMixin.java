package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiExtractMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void polytone$startOverlayModifiers(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci) {
        Polytone.OVERLAY_MODIFIERS.onStartRenderingOverlay();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void polytone$endOverlayModifiers(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci) {
        Polytone.OVERLAY_MODIFIERS.onEndRenderingOverlay();
    }
}
