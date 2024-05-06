package net.mehvahdjukaar.polytone.mixins.fabric;

import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.mixin.client.indigo.renderer.ItemRendererMixin;
import net.mehvahdjukaar.polytone.slotify.SlotifyScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin implements SlotifyScreen {

    @ModifyArg(method = "renderBg", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIFFLnet/minecraft/world/entity/LivingEntity;)V")
            , index = 1
    )
    public int modifyRenderEntityX(int x) {
        var m = this.polytone$getModifier();
        if (m != null) {

            var s = m.getSpecial("player");
            if (s != null) {
                return x + s.x();
            }
        }
        return x;
    }

    @ModifyArg(method = "renderBg", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIFFLnet/minecraft/world/entity/LivingEntity;)V")
            , index = 2
    )
    public int modifyRenderEntityY(int y) {
        var m = this.polytone$getModifier();
        if (m != null) {
            var s = m.getSpecial("player");
            if (s != null) {
                return y + s.y();
            }
        }
        return y;
    }

    @Inject(method = "method_19891", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;setPosition(II)V",
            shift = At.Shift.AFTER))
    private void polytone$onInit(Button button, CallbackInfo ci) {
        var mod = this.polytone$getModifier();
        if (mod != null) {
            mod.modifyWidgets(button);
        }
    }
}
