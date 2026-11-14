package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.world.entity.ai.behavior.CelebrateVillagersSurvivedRaid;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CelebrateVillagersSurvivedRaid.class)
public class CelebrateRaidFireworkMixin {

    @WrapOperation(method = "getFirework(Lnet/minecraft/world/item/DyeColor;I)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/DyeColor;getFireworkColor()I"))
    private int polytone$storeVanillaColor(DyeColor dye, Operation<Integer> original) {
        return Polytone.COLORS.getVanillaFireworkColor(dye);
    }
}
