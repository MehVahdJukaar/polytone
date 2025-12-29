package net.mehvahdjukaar.polytone.mixins;

import net.minecraft.client.renderer.block.model.FaceBakery;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FaceBakery.class)
public class FaceBakeryMixin {
//    I do not believe this needs to be done anymore in 1.21.11+ - the code appears to simply take the rotation as-is and apply it.
//    @WrapOperation(method = "applyElementRotation", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;rotation(FLorg/joml/Vector3fc;)Lorg/joml/Matrix4f;"))
//    private static Matrix4f polytone$unRestrictRotationAngle(Matrix4f instance, float angle, Vector3fc axis, Operation<Matrix4f> original,
//                                                        @Nullable @Local(argsOnly = true) BlockElementRotation elementRot) {
//        if (elementRot != null) {
//            Vector3fc rot = ((IExtendedBlockElementRotation) (Object) elementRot).getRotation();
//            if (rot != null) {
//                return instance.rotateXYZ(Mth.DEG_TO_RAD * rot.x(), Mth.DEG_TO_RAD * rot.y(), Mth.DEG_TO_RAD * rot.z());
//            }
//        }
//        return original.call(instance, angle, axis);
//    }
}
