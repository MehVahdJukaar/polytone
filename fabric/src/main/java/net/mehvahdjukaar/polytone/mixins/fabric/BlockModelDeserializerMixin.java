package net.mehvahdjukaar.polytone.mixins.fabric;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.polytone.utils.fabric.SeparateTransformsModel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.lang.reflect.Type;

@Mixin(BlockModel.Deserializer.class)
public abstract class BlockModelDeserializerMixin {

    @ModifyReturnValue(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/renderer/block/model/BlockModel;",
            at = @At("RETURN"))
    public BlockModel deserialize(BlockModel original, JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonobject = element.getAsJsonObject();
        if (jsonobject.has("loader")) {
            String loader = GsonHelper.getAsString(jsonobject, "loader");
            BlockModel custom = SeparateTransformsModel.readModel(
                    loader, context, jsonobject, original);
            if (custom != null) return custom;
        }
        return original;
    }
}
