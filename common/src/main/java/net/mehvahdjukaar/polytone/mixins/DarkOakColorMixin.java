package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Biome.class)
public class DarkOakColorMixin {

    @Shadow
    @Final
    private BiomeSpecialEffects specialEffects;

    @ModifyReturnValue(method = "getGrassColor", at = @At("RETURN"))
    private int poly$modifyColor(int original, @Local(ordinal = 0) int baseGrassColor) {
        if (specialEffects.grassColorModifier() != BiomeSpecialEffects.GrassColorModifier.DARK_FOREST) return original;
        Integer a = Polytone.COLORS.getSpecialSwampDark();
        if (a != null) {
            return ColorUtils.blendColor(baseGrassColor, a);
        }
        return original;
    }
}
