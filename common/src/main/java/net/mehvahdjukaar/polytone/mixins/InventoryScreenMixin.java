package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.slotify.SlotifyScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//for recipe button
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin implements SlotifyScreen {

    @Inject(method = "method_19891", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;setPosition(II)V",
            shift = At.Shift.AFTER))
    private void polytone$onInit(Button button, CallbackInfo ci) {
        var mod = this.polytone$getModifier();
        if (mod != null) {
           mod.modifyWidgets(button);
        }

    }
}