package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.world.item.alchemy.PotionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PotionUtils.class)
public class PotionUtilsMixin {


    @ModifyReturnValue(method = "getColor(Lnet/minecraft/world/item/alchemy/Potion;)I", at = @At("RETURN"))
    private static int polytone$modifyWaterColor(int original) {
        if (original == 16253176) return Polytone.COLORS.getEmptyPot();
        return original;
    }

    @ModifyReturnValue(method = "getColor(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"))
    private static int polytone$modifyWaterColor2(int original) {
        if (original == 16253176) return Polytone.COLORS.getEmptyPot();
        return original;
    }

    @ModifyReturnValue(method = "getColor(Ljava/util/Collection;)I", at = @At(value = "RETURN", ordinal = 0))
    private static int polytone$modifyWaterColor3(int original) {
        return Polytone.COLORS.getWaterBottle();
    }
}
