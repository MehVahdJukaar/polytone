package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.CubicSampler;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @WrapOperation(method = "setupColor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/CubicSampler;gaussianSampleVec3(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/util/CubicSampler$Vec3Fetcher;)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 polytone$modifyFogColor(Vec3 center, CubicSampler.Vec3Fetcher fetcher,
                                                Operation<Vec3> original, @Local(argsOnly = true) ClientLevel level,
                                                @Local(argsOnly = true) int renderDistanceChunks,
                                                @Local(ordinal = 6) float lightLevel) {
        // Polytone.DIMENSION_MODIFIERS. modifyFogMagicNumber((float) renderDistanceChunks, distance);

        Vec3 modified = Polytone.DIMENSION_MODIFIERS.modifyFogColor(center, level, lightLevel);
        if (modified != null) return modified;
        return original.call(center, fetcher);
    }


    @ModifyExpressionValue(method = "setupColor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getWaterVision()F"))
    private static float polytone$scaleWaterVision(float original) {
        return original * Polytone.COLORS.getWaterFogBrightening();
    }

    @ModifyExpressionValue(method = "setupColor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
    private static float polytone$modifyRainFog(float original, @Local(argsOnly = true) ClientLevel level) {
        if (original != 0 && Polytone.DIMENSION_MODIFIERS.shouldCancelFogWeatherDarken(level)) {
            return 0;
        }
        return original;
    }

    @ModifyExpressionValue(method = "setupColor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getThunderLevel(F)F"))
    private static float polytone$modifyThunderFog(float original, @Local(argsOnly = true) ClientLevel level) {
        if (original != 0 && Polytone.DIMENSION_MODIFIERS.shouldCancelFogWeatherDarken(level)) {
            return 0;
        }
        return original;
    }

    @ModifyConstant(method = "setupColor", constant = @Constant(floatValue = 0.623F))
    private static float polytone$powderSnowFogRed(float original) {
        Integer custom = Polytone.COLORS.getPowderSnowFogColor();
        return custom != null ? (custom >> 16 & 255) / 255f : original;
    }

    @ModifyConstant(method = "setupColor", constant = @Constant(floatValue = 0.734F))
    private static float polytone$powderSnowFogGreen(float original) {
        Integer custom = Polytone.COLORS.getPowderSnowFogColor();
        return custom != null ? (custom >> 8 & 255) / 255f : original;
    }

    @ModifyConstant(method = "setupColor", constant = @Constant(floatValue = 0.785F))
    private static float polytone$powderSnowFogBlue(float original) {
        Integer custom = Polytone.COLORS.getPowderSnowFogColor();
        return custom != null ? (custom & 255) / 255f : original;
    }
}
