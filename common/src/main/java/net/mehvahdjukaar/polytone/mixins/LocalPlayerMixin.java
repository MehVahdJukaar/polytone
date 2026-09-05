package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @ModifyConstant(method = "getWaterVision", constant = {@Constant(floatValue = 600.0F),
            @Constant(floatValue = 100.0F), @Constant(floatValue = 500.0F)})
    private float polytone$stretchWaterVisionRamp(float constant) {
        return constant * Polytone.COLORS.getWaterVisionTimeScale();
    }
}
