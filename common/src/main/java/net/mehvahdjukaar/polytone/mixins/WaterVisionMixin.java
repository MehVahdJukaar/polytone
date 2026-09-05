package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FogRenderer.class)
public class WaterVisionMixin {

    @ModifyExpressionValue(method = "computeFogColor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getWaterVision()F"))
    private float polytone$scaleWaterVision(float original) {
        return original * Polytone.COLORS.getWaterFogBrightening();
    }
}
