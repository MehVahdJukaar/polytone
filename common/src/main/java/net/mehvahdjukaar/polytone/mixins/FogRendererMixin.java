package net.mehvahdjukaar.polytone.mixins;

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

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @WrapOperation(method = "setupColor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/CubicSampler;gaussianSampleVec3(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/util/CubicSampler$Vec3Fetcher;)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 polytone$modifyFogColor(Vec3 center, CubicSampler.Vec3Fetcher fetcher,
                                       Operation<Vec3> original, @Local ClientLevel level,
                                       @Local(ordinal = 4) float lightLevel) {
        Vec3 modified = Polytone.DIMENSION_EFFECTS.modifyFogColor(center, level, lightLevel);
        if (modified != null) return modified;
        return original.call(center, fetcher);
    }
}
