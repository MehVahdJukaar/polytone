package net.mehvahdjukaar.polytone.mixins;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.util.GsonHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Variant.Deserializer.class)
public class VariantDeserializerMixin {

    @ModifyExpressionValue(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/renderer/block/model/Variant;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/model/BlockModelRotation;getRotation()Lcom/mojang/math/Transformation;")
    )
    public Transformation polytone$addTranslation(Transformation original, @Local JsonObject jsonObject) {
        float x = GsonHelper.getAsFloat(jsonObject, "xoffset", 0);
        float y = GsonHelper.getAsFloat(jsonObject, "yoffset", 0);
        float z = GsonHelper.getAsFloat(jsonObject, "zoffset", 0);
        if (x == 0 && y == 0 && z == 0) return original;
        Matrix4f mat = new Matrix4f();
        mat.translate(x /16f, y /16f, z /16f);
        return new Transformation(mat).compose(original);
    }
}
