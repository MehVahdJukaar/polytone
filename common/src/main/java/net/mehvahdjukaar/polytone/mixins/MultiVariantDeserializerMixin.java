package net.mehvahdjukaar.polytone.mixins;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.content.expmodel.ExpressionModel;
import net.minecraft.client.renderer.block.model.MultiVariant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

@Mixin(MultiVariant.Deserializer.class)
public class MultiVariantDeserializerMixin {

    @Inject(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/renderer/block/model/MultiVariant;",
            at = @At("HEAD"), cancellable = true)
    private void polytone$expressionModel(JsonElement json, Type type, JsonDeserializationContext context,
                                          CallbackInfoReturnable<MultiVariant> cir) {
        if (ExpressionModel.isExpressionModel(json)) {
            cir.setReturnValue(ExpressionModel.parse(json.getAsJsonObject(), context));
        }
    }
}
