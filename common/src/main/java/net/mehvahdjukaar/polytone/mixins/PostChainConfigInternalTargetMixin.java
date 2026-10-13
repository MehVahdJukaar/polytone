package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.content.shaders.IScaledTarget;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.function.Function;

@Mixin(PostChainConfig.InternalTarget.class)
public class PostChainConfigInternalTargetMixin implements IScaledTarget {
    @Unique
    private @Nullable Float polytone$scale = null;

    @Override
    public @Nullable Float polytone$getScale() {
        return this.polytone$scale;
    }

    @Override
    public void polytone$setScale(@Nullable Float scale) {
        this.polytone$scale = scale;
    }

    @ModifyExpressionValue(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    remap = false,
                    target = "com/mojang/serialization/codecs/RecordCodecBuilder.create(Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"
            )
    )
    private static Codec<PostChainConfig.InternalTarget> extendCodec(Codec<PostChainConfig.InternalTarget> original) {
        return RecordCodecBuilder.create(instance ->
                instance.group(
                                MapCodec.assumeMapUnsafe(original).forGetter(Function.identity()),
                                ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("scale").forGetter(t ->
                                        Optional.ofNullable(((IScaledTarget) (Object) t).polytone$getScale()))
                        )
                        .apply(instance, (target, scale) -> {
                            ((IScaledTarget) (Object) target).polytone$setScale(scale.orElse(null));
                            return target;
                        })
        );
    }
}
