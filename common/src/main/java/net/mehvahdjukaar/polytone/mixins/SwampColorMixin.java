package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net/minecraft/world/level/biome/BiomeSpecialEffects$GrassColorModifier$3")
public class SwampColorMixin {

    @Inject(method = "modifyColor", at = @At("HEAD"), cancellable = true)
    public void poly$modifyColor(double d, double e, int i, CallbackInfoReturnable<Integer> cir) {
        Integer a = Polytone.COLORS.getSpecialSwampLight();
        Integer b = Polytone.COLORS.getSpecialSwampDark();
        if(a != null || b != null) {
            a = a == null ? 5011004 : a;
            b = b == null ? 6975545 : b;
            double f = Biome.BIOME_INFO_NOISE.getValue(d * 0.0225, e * 0.0225, false);
            cir.setReturnValue(f < -0.1 ? a : b);
        }
    }
}
