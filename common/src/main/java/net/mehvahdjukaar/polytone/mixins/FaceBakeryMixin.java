package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.utils.IExtendedBlockElementRotation;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FaceBakery.class)
public class FaceBakeryMixin {

    @WrapOperation(method = "applyElementRotation", at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationAxis(FLorg/joml/Vector3fc;)Lorg/joml/Quaternionf;"))
    private static Quaternionf polytone$unRestrictRotationAngle(Quaternionf instance, float angle, Vector3fc axis, Operation<Quaternionf> original,
                                                                @Local(argsOnly = true) BlockElementRotation partRotation) {
        Vector3fc rot = ((IExtendedBlockElementRotation) (Object) partRotation).getRotation();
        if (rot != null) {
            return instance.rotateZYX(Mth.DEG_TO_RAD * rot.z(), Mth.DEG_TO_RAD * rot.y(), Mth.DEG_TO_RAD * rot.x());
        }
        return original.call(instance, angle, axis);
    }
}
