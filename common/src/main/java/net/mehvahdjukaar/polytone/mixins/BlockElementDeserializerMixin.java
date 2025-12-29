package net.mehvahdjukaar.polytone.mixins;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.util.GsonHelper;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockElement.Deserializer.class)
public class BlockElementDeserializerMixin {

    // Angle is no longer restricted - it is turned into a single axis rotation without any limitation
//    @Inject(method = "getAngle", at = @At("HEAD"), cancellable = true)
//    public void polytone$unRestrictAngles(JsonObject json, CallbackInfoReturnable<Float> cir) {
//        float f = GsonHelper.getAsFloat(json, "angle");
//        cir.setReturnValue(f);
//    }

    @Inject(method = "getRotation", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/model/BlockElement$Deserializer;getAxis(Lcom/google/gson/JsonObject;)Lnet/minecraft/core/Direction$Axis;",
            shift = At.Shift.BEFORE), cancellable = true)
    public void polytone$unRestrictRotation(JsonObject json, CallbackInfoReturnable<BlockElementRotation> cir,
                                            @Local(ordinal = 1) JsonObject rotObj, @Local Vector3f offset) {
        if (rotObj.has("x") || rotObj.has("y") || rotObj.has("z")) {
            boolean bl = false; // not supported
            float x = GsonHelper.getAsFloat(rotObj, "x", 0);
            float y = GsonHelper.getAsFloat(rotObj, "y", 0);
            float z = GsonHelper.getAsFloat(rotObj, "z", 0);
            var eulerValue = new BlockElementRotation.EulerXYZRotation(x, y, z);
            var blockElementRotation = new BlockElementRotation(offset, eulerValue, bl);
            cir.setReturnValue(blockElementRotation);
        }
    }
}
