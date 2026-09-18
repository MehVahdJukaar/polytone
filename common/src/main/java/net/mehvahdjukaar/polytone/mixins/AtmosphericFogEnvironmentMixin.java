package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(AtmosphericFogEnvironment.class)
public class AtmosphericFogEnvironmentMixin {

    @ModifyExpressionValue(method = "setupFog", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/client/renderer/fog/environment/AtmosphericFogEnvironment;rainFogMultiplier:F"))
    private float polytone$scaleRainFog(float original) {
        return original * Polytone.DIMENSION_MODIFIERS.getRainFogStrength();
    }

    @ModifyExpressionValue(method = "getBaseColor", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"),
            @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getThunderLevel(F)F")})
    private float polytone$skipWeatherDarken(float original) {
        return Polytone.DIMENSION_MODIFIERS.noWeatherFogDarken() ? 0 : original;
    }

    @ModifyConstant(method = "setupFog", constant = @Constant(floatValue = 96.0F, ordinal = 1))
    private float polytone$bossFogEnd(float original) {
        return Polytone.COLORS.getBossFogDistance(original);
    }

    @ModifyConstant(method = "setupFog", constant = @Constant(floatValue = 10.0F))
    private float polytone$bossFogStart(float original) {
        return Polytone.COLORS.getBossFogDistance(96) * original / 96;
    }
}
