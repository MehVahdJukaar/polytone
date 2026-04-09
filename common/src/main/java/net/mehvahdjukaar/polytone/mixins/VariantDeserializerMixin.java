package net.mehvahdjukaar.polytone.mixins;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Variant.Deserializer.class)
public class VariantDeserializerMixin {

    @WrapOperation(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/renderer/block/model/Variant;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/model/Variant$Deserializer;getBlockRotation(Lcom/google/gson/JsonObject;)Lnet/minecraft/client/resources/model/BlockModelRotation;"))
    public BlockModelRotation polytone$cancelVanillaTransformation(Variant.Deserializer instance, JsonObject json, Operation<BlockModelRotation> original) {
        return BlockModelRotation.by(0, 0);
    }

    @WrapOperation(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/renderer/block/model/Variant;",
            at = @At(value = "NEW",
                    target = "(Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/math/Transformation;ZI)Lnet/minecraft/client/renderer/block/model/Variant;")
    )
    public Variant polytone$addTranslation(ResourceLocation modelLocation, Transformation rotation, boolean uvLock, int weight, Operation<Variant> op,
                                           @Local JsonObject jsonObject) {
        float xOffset = GsonHelper.getAsFloat(jsonObject, "xoffset", 0);
        float yOffset = GsonHelper.getAsFloat(jsonObject, "yoffset", 0);
        float zOffset = GsonHelper.getAsFloat(jsonObject, "zoffset", 0);
        float xRot = GsonHelper.getAsFloat(jsonObject, "x", 0);
        float yRot = GsonHelper.getAsFloat(jsonObject, "y", 0);
        float zRot = GsonHelper.getAsFloat(jsonObject, "z", 0);

        if (xOffset != 0 || yOffset != 0 || zOffset != 0 || zRot != 0 || xRot % 45 != 0 || yRot % 45 != 0) {

            Matrix4f mat = new Matrix4f();
            Quaternionf quaternionf = (new Quaternionf())
                    .rotateYXZ(-yRot * Mth.DEG_TO_RAD,
                            -xRot * Mth.DEG_TO_RAD, -zRot * Mth.DEG_TO_RAD);
            mat = mat.translate(xOffset / 16f, yOffset / 16f, zOffset / 16f);
            mat = mat.rotate(quaternionf);

            rotation = new Transformation(mat);
        } else {
            BlockModelRotation blockModelRotation = BlockModelRotation.by((int) xRot, (int) yRot);
            rotation = blockModelRotation.getRotation();
        }

        return op.call(modelLocation, rotation, uvLock, weight);
    }

}
