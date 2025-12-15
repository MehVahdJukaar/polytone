package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.misc.IExtendedBlockElementRotation;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FaceBakery.class)
public class FaceBakeryMixin {

    @WrapOperation(method = "applyElementRotation", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;rotation(FLorg/joml/Vector3fc;)Lorg/joml/Matrix4f;"))
    private static Matrix4f polytone$unRestrictRotationAngle(Matrix4f instance, float angle, Vector3fc axis, Operation<Matrix4f> original,
                                                        @Nullable @Local(argsOnly = true) BlockElementRotation elementRot) {
        if (elementRot != null) {
            Vector3fc rot = ((IExtendedBlockElementRotation) (Object) elementRot).getRotation();
            if (rot != null) {
                return instance.rotateXYZ(Mth.DEG_TO_RAD * rot.x(), Mth.DEG_TO_RAD * rot.y(), Mth.DEG_TO_RAD * rot.z());
            }
        }
        return original.call(instance, angle, axis);
    }
}
