package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.slotify.SlotifyScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SmithingScreen.class)
public class SmithingScreenMixin {


    @ModifyArg(method = "renderBg", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventory(Lnet/minecraft/client/gui/GuiGraphics;FFFLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V")
            , index = 1
    )
    public float modifyRenderEntityX(float x) {
        var m = ((SlotifyScreen) this).polytone$getModifier();
        if (m != null) {

            var s = m.getSpecial("player");
            if (s != null) {
                return x + s.x();
            }
        }
        return x;
    }

    @ModifyArg(method = "renderBg", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventory(Lnet/minecraft/client/gui/GuiGraphics;FFFLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V")
            , index = 2
    )
    public float modifyRenderEntityY(float y) {
        var m = ((SlotifyScreen) this).polytone$getModifier();
        if (m != null) {
            var s = m.getSpecial("player");
            if (s != null) {
                return y + s.y();
            }
        }
        return y;
    }

    @ModifyArg(method = "renderBg", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventory(Lnet/minecraft/client/gui/GuiGraphics;FFFLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V")
            , index = 3
    )
    public float modifyRenderEntityScale(float z) {
        var m = ((SlotifyScreen) this).polytone$getModifier();
        if (m != null) {
            var s = m.getSpecial("player");
            if (s != null) {
                return z + s.z();
            }
        }
        return z;
    }
}
