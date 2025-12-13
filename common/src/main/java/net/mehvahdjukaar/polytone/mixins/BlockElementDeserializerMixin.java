package net.mehvahdjukaar.polytone.mixins;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.utils.IExtendedBlockElementRotation;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockElement.Deserializer.class)
public class BlockElementDeserializerMixin {


    @Inject(method = "getAngle", at = @At("HEAD"), cancellable = true)
    public void polytone$unRestrictAngles(JsonObject json, CallbackInfoReturnable<Float> cir) {
        float f = GsonHelper.getAsFloat(json, "angle");
        cir.setReturnValue(f);
    }

    @Inject(method = "getRotation", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/model/BlockElement$Deserializer;getAxis(Lcom/google/gson/JsonObject;)Lnet/minecraft/core/Direction$Axis;",
            shift = At.Shift.BEFORE))
    public void polytone$unRestrictRotation(JsonObject json, CallbackInfoReturnable<BlockElementRotation> cir,
                                            @Local(ordinal = 1) JsonObject rotObj, @Local Vector3f offset) {
        if (rotObj.has("x") || rotObj.has("y") || rotObj.has("z")) {
            boolean bl = false; // not supported
            var blockElementRotation = new BlockElementRotation(offset, Direction.Axis.Y, 0, bl);
            float x = GsonHelper.getAsFloat(rotObj, "x", 0);
            float y = GsonHelper.getAsFloat(rotObj, "y", 0);
            float z = GsonHelper.getAsFloat(rotObj, "z", 0);
            var rot = new Vector3f(x, y, z);
            ((IExtendedBlockElementRotation)(Object) blockElementRotation).setRotation(rot);
            cir.setReturnValue(blockElementRotation);
        }
    }
}
