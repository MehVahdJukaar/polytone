package net.mehvahdjukaar.polytone.mixins;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.resources.model.BlockModelRotation;
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
    public BlockModelRotation polytone$cancelVanillaTransformation(Variant.Deserializer instance, JsonObject json, Operation<BlockModelRotation> original){
        return BlockModelRotation.by(0,0);
    }

    @ModifyExpressionValue(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/renderer/block/model/Variant;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/model/BlockModelRotation;getRotation()Lcom/mojang/math/Transformation;")
    )
    public Transformation polytone$addTranslation(Transformation original, @Local JsonObject jsonObject) {
        float xOffset = GsonHelper.getAsFloat(jsonObject, "xoffset", 0);
        float yOffset = GsonHelper.getAsFloat(jsonObject, "yoffset", 0);
        float zOffset = GsonHelper.getAsFloat(jsonObject, "zoffset", 0);
        float xRot = GsonHelper.getAsFloat(jsonObject, "x", 0);
        float yRot = GsonHelper.getAsFloat(jsonObject, "y", 0);
        float zRot = GsonHelper.getAsFloat(jsonObject, "z", 0);
        Matrix4f mat = new Matrix4f();
        Quaternionf quaternionf = (new Quaternionf())
                .rotateYXZ(-yRot * Mth.DEG_TO_RAD,
                        -xRot * Mth.DEG_TO_RAD, -zRot * Mth.DEG_TO_RAD);
        mat.rotate(quaternionf);
        mat.translate(xOffset / 16f, yOffset / 16f, zOffset / 16f);
        return new Transformation(mat);
    }

}
