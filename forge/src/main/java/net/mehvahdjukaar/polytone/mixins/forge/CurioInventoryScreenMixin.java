package net.mehvahdjukaar.polytone.mixins.forge;

import net.mehvahdjukaar.polytone.slotify.SlotifyScreen;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraftforge.client.model.SeparateTransformsModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import top.theillusivec4.curios.client.gui.CuriosScreen;

@Pseudo
@Mixin(CuriosScreen.class)
public abstract class CurioInventoryScreenMixin {

    @ModifyArg(method = "renderBg", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIFFLnet/minecraft/world/entity/LivingEntity;)V")
            , index = 1
    )
    public int modifyRenderEntityX(int x) {
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
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIFFLnet/minecraft/world/entity/LivingEntity;)V")
            , index = 2
    )
    public int modifyRenderEntityY(int y) {
        var m = ((SlotifyScreen) this).polytone$getModifier();
        if (m != null) {
            var s = m.getSpecial("player");
            if (s != null) {
                return y + s.y();
            }
        }
        return y;
    }


}
