package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.CubicSampler;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @WrapOperation(method = "setupColor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/CubicSampler;gaussianSampleVec3(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/util/CubicSampler$Vec3Fetcher;)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 polytone$modifyFogColor(Vec3 center, CubicSampler.Vec3Fetcher fetcher,
                                                Operation<Vec3> original, @Local(argsOnly = true) ClientLevel level,
                                                @Local(argsOnly = true) int renderDistanceChunks,
                                                @Local(ordinal = 4) float lightLevel,
                                                @Local(ordinal = 2) LocalFloatRef distance) {
      //  Polytone.DIMENSION_MODIFIERS. modifyFogMagicNumber((float) renderDistanceChunks, distance);

        Vec3 modified = Polytone.DIMENSION_MODIFIERS.modifyFogColor(center, level, lightLevel);
        if (modified != null) return modified;
        return original.call(center, fetcher);
    }

}
