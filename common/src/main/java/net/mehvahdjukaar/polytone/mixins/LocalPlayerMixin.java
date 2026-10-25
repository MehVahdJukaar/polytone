package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @ModifyExpressionValue(method = "getWaterVision", at = {@At(value = "CONSTANT", args = "floatValue=600.0F"),
            @At(value = "CONSTANT", args = "floatValue=100.0F"), @At(value = "CONSTANT", args = "floatValue=500.0F")})
    private float polytone$stretchWaterVisionRamp(float constant) {
        return constant * Polytone.COLORS.getWaterVisionTimeScale();
    }
}
