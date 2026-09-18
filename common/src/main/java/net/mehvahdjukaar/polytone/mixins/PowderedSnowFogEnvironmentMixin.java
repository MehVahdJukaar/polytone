package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.fog.environment.PowderedSnowFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PowderedSnowFogEnvironment.class)
public class PowderedSnowFogEnvironmentMixin {

    @ModifyReturnValue(method = "getBaseColor", at = @At("RETURN"))
    private int polytone$customPowderSnowFog(int original) {
        Integer custom = Polytone.COLORS.getPowderSnowFogColor();
        return custom != null ? custom : original;
    }
}
