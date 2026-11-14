package net.mehvahdjukaar.polytone.mixins;

import it.unimi.dsi.fastutil.ints.IntList;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.color.item.Firework;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Firework.class)
public class FireworkTintMixin {

    @ModifyVariable(method = "calculate(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/LivingEntity;)I",
            at = @At("STORE"))
    private IntList polytone$remapColors(IntList colors) {
        return Polytone.COLORS.remapFireworkColors(colors);
    }
}
