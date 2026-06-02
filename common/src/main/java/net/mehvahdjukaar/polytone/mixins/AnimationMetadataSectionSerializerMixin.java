package net.mehvahdjukaar.polytone.mixins;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.content.texture.IDeltaProvider;
import net.mehvahdjukaar.polytone.content.texture.IDeltaProviderContext;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSectionSerializer;
import net.minecraft.util.GsonHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnimationMetadataSectionSerializer.class)
public class AnimationMetadataSectionSerializerMixin {


    @ModifyReturnValue(method = "fromJson(Lcom/google/gson/JsonObject;)Lnet/minecraft/client/resources/metadata/animation/AnimationMetadataSection;", at = @At("RETURN"))
    public AnimationMetadataSection polytone$addWorldTimeTextureData(AnimationMetadataSection original,
                                                                     JsonObject json) {
        JsonElement mode1 = json.get("mode");
        if (mode1 != null) {
            IDeltaProvider.CODEC.parse(JsonOps.INSTANCE, mode1).result()
                    .ifPresent(mode -> ((IDeltaProviderContext) original)
                            .polytone$setMode(mode));
        }
        if (json.has("time_cycle_duration")) {
            ((IDeltaProviderContext) original).polytone$setTimeCycleDuration(GsonHelper.getAsInt(json, "time_cycle_duration"));
        }
        return original;
    }
}
