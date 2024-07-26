package net.mehvahdjukaar.polytone.mixins;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.polytone.texture.DayTimeTexture;
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
        if (GsonHelper.getAsBoolean(json, "use_day_time", false)) {
            ((DayTimeTexture) original).polytone$setUsesDayTime(true);
        }
        return original;
    }
}
