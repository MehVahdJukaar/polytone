package net.mehvahdjukaar.polytone.mixins;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.texture.DayTimeTexture;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSectionSerializer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.GsonHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnimationMetadataSectionSerializer.class)
public class AnimationMetadataSectionSerializerMixin {

    public static final Codec<AnimationMetadataSection> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(
                AnimationFrame.CODEC.listOf().optionalFieldOf("frames").forGetter(AnimationMetadataSection::frames),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("width").forGetter(AnimationMetadataSection::frameWidth),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("height").forGetter(AnimationMetadataSection::frameHeight),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("frametime", 1).forGetter(AnimationMetadataSection::defaultFrameTime),
                Codec.BOOL.optionalFieldOf("interpolate", false).forGetter(AnimationMetadataSection::interpolatedFrames)

        ).apply(instance, AnimationMetadataSection::new);
    });

    @ModifyReturnValue(method = "fromJson(Lcom/google/gson/JsonObject;)Lnet/minecraft/client/resources/metadata/animation/AnimationMetadataSection;", at = @At("RETURN"))
    public AnimationMetadataSection polytone$addWorldTimeTextureData(AnimationMetadataSection original,
                                                                     JsonObject json) {
        DayTimeTexture.Mode mode = DayTimeTexture.Mode.get(json.get("mode"));
        if (mode != null) {
            AnimationMetadataSection
                    ((DayTimeTexture) original).polytone$setMode(mode);
        }
        if (json.has("time_cycle_duration")) {
            ((DayTimeTexture) original).polytone$setTimeCycleDuration(GsonHelper.getAsInt(json, "time_cycle_duration"));
        }
        return original;
    }
}
