package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FaceBakery.class)
public class FaceBakeryMixin {

    @WrapOperation(method = "applyElementRotation", at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationAxis(FLorg/joml/Vector3fc;)Lorg/joml/Quaternionf;"))
    public Quaternionf polytone$unRestrictRotationAngle(Quaternionf instance, float angle, Vector3fc axis, Operation<Quaternionf> original,
                                                        @Local(argsOnly = true) BlockElementRotation partRotation) {
        Vector3fc rot = ((IExtendedBlockElementRotation) (Object) partRotation).getRotation();
        if (rot != null) {
            return instance.rotateZYX(Mth.DEG_TO_RAD * rot.z(), Mth.DEG_TO_RAD * rot.y(), Mth.DEG_TO_RAD * rot.x());
        }
        return original.call(instance, angle, axis);
    }

    @WrapWithCondition(method = "bakeQuad", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/model/FaceBakery;recalculateWinding([ILnet/minecraft/core/Direction;)V"))
    public boolean poly$allowArbitraryRot(FaceBakery instance, int[] vertices, Direction direction,
                                          @Local(argsOnly = true) ModelState transform) {
        var t = transform.getRotation();
        if (t == Transformation.identity()) return true;
        if (transform instanceof IVariantExtension variant) {
            return !variant.poly$isUncapped();
        }
        return true;
    }


}
