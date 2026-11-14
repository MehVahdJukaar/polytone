package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.crafting.FireworkStarFadeRecipe;
import net.minecraft.world.item.crafting.FireworkStarRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//we must unmap our integrated server changs so a possible future pack reload can apply changes to already crafted stuff
@Mixin({FireworkStarRecipe.class, FireworkStarFadeRecipe.class})
public class FireworkRecipesMixin {

    @WrapOperation(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/DyeColor;getFireworkColor()I"))
    private int polytone$storeVanillaColor(DyeColor dye, Operation<Integer> original) {
        return Polytone.COLORS.getVanillaFireworkColor(dye);
    }
}
