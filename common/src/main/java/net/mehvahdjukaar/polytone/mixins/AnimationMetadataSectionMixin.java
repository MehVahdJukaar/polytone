package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.content.texture.IDeltaProvider;
import net.mehvahdjukaar.polytone.content.texture.IDeltaProviderContext;
import net.minecraft.SharedConstants;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

@Mixin(AnimationMetadataSection.class)
public class AnimationMetadataSectionMixin implements IDeltaProviderContext {
    @Unique
    private IDeltaProvider polytone$mode = IDeltaProvider.PresetProvider    .VANILLA;
    @Unique
    private int polytone$dayDuration = SharedConstants.TICKS_PER_GAME_DAY;

    @Override
    public IDeltaProvider polytone$getDeltaProvider() {
        return this.polytone$mode;
    }

    @Override
    public void polytone$setDeltaProvider(IDeltaProvider mode) {
        this.polytone$mode = mode;
    }

    @Override
    public int polytone$getTimeCycleDuration() {
        return polytone$dayDuration;
    }

    @Override
    public void polytone$setTimeCycleDuration(int duration) {
        this.polytone$dayDuration = duration;
    }


    @ModifyExpressionValue(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    remap = false,
                    target = "com/mojang/serialization/codecs/RecordCodecBuilder.create(Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"
            )
    )
    private static Codec<AnimationMetadataSection> extendCodec(Codec<AnimationMetadataSection> original) {
        return RecordCodecBuilder.create(instance ->
                instance.group(
                                MapCodec.assumeMapUnsafe(original).forGetter(Function.identity()),
                                IDeltaProvider.CODEC.optionalFieldOf("mode", IDeltaProvider.PresetProvider.VANILLA).forGetter(a->
                                        ((IDeltaProviderContext)(Object)a).polytone$getDeltaProvider()),
                                Codec.INT.optionalFieldOf("time_cycle_duration", SharedConstants.TICKS_PER_GAME_DAY).forGetter(a->
                                        ((IDeltaProviderContext)(Object)a).polytone$getTimeCycleDuration())
                        )
                        .apply(instance, (typeInstance, mode, time) -> {
                            ((IDeltaProviderContext)(Object)typeInstance).polytone$setDeltaProvider(mode);
                            ((IDeltaProviderContext)(Object)typeInstance).polytone$setTimeCycleDuration(time);
                            return typeInstance;
                        })
        );
    }
}
