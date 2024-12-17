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
        DayTimeTexture.Mode mode = DayTimeTexture.Mode.get(json.get("mode"));
        if (mode != null) {
            ((DayTimeTexture) original).polytone$setMode(mode);
        }
        if (json.has("time_cycle_duration")) {
            ((DayTimeTexture) original).polytone$setTimeCycleDuration(GsonHelper.getAsInt(json, "time_cycle_duration"));
        }
        return original;
    }
}
