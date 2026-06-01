package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.core.Direction;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FaceBakery.class)
public class FaceBakeryMixin {

    @WrapWithCondition(method = "bakeQuad", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/cuboid/FaceBakery;recalculateWinding([Lorg/joml/Vector3fc;[JLnet/minecraft/core/Direction;)V"))
    private static boolean poly$applyNeoFixForArbitraryRotations(Vector3fc[] vector3fcs, long[] ls, Direction direction,
                                                                 @Local(argsOnly = true) ModelState modelState) {

        // Suppress winding re-calculation when the quads may not be axis-aligned due to root transforms
        return !poly$mayApplyArbitraryRotation(modelState);
    }

    @Unique
    private static final Class<?> poly$WITH_UV_LOCK_CLASS = BlockModelRotation.IDENTITY.withUvLock().getClass();

    @Unique
    private static boolean poly$mayApplyArbitraryRotation(ModelState self) {
        return !(self instanceof BlockModelRotation) && !poly$WITH_UV_LOCK_CLASS.isInstance(self);
    }
}
