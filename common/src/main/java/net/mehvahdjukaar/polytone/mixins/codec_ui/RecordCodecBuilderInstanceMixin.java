package net.mehvahdjukaar.polytone.mixins.codec_ui;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.codec_ui.internal.RecordFieldTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Propagates accumulated field tags through the applicative apN combinators.
 * Each {@code apN} takes an applicative function plus N field-carrying RCBs and produces a
 * new RCB whose tag list is the concatenation of the inputs' tag lists. The applicative
 * function position ({@code func}) typically carries no field tags itself (it's built from
 * {@code point}); concat just skips empty contributions.
 */
@Mixin(RecordCodecBuilder.Instance.class)
public abstract class RecordCodecBuilderInstanceMixin {

    @SuppressWarnings("rawtypes")
    @ModifyReturnValue(method = "ap2", at = @At("RETURN"))
    private App<?, ?> polytone$tagAp2(App<?, ?> result,
                                       @Local(argsOnly = true, ordinal = 0) App func,
                                       @Local(argsOnly = true, ordinal = 1) App a,
                                       @Local(argsOnly = true, ordinal = 2) App b) {
        polytone$concat(result, func, a, b);
        return result;
    }

    @SuppressWarnings("rawtypes")
    @ModifyReturnValue(method = "ap3", at = @At("RETURN"))
    private App<?, ?> polytone$tagAp3(App<?, ?> result,
                                       @Local(argsOnly = true, ordinal = 0) App func,
                                       @Local(argsOnly = true, ordinal = 1) App t1,
                                       @Local(argsOnly = true, ordinal = 2) App t2,
                                       @Local(argsOnly = true, ordinal = 3) App t3) {
        polytone$concat(result, func, t1, t2, t3);
        return result;
    }

    @SuppressWarnings("rawtypes")
    @ModifyReturnValue(method = "ap4", at = @At("RETURN"))
    private App<?, ?> polytone$tagAp4(App<?, ?> result,
                                       @Local(argsOnly = true, ordinal = 0) App func,
                                       @Local(argsOnly = true, ordinal = 1) App t1,
                                       @Local(argsOnly = true, ordinal = 2) App t2,
                                       @Local(argsOnly = true, ordinal = 3) App t3,
                                       @Local(argsOnly = true, ordinal = 4) App t4) {
        polytone$concat(result, func, t1, t2, t3, t4);
        return result;
    }

    @Unique
    @SuppressWarnings("rawtypes")
    private static void polytone$concat(App<?, ?> result, App... inputs) {
        try {
            if (!(result instanceof RecordCodecBuilder<?, ?> resultBuilder)) return;
            RecordCodecBuilder<?, ?>[] in = new RecordCodecBuilder[inputs.length];
            for (int i = 0; i < inputs.length; i++) {
                if (inputs[i] instanceof RecordCodecBuilder<?, ?> rcb) {
                    in[i] = rcb;
                }
            }
            RecordFieldTags.concat(resultBuilder, in);
        } catch (Throwable ignored) {
            // Best-effort.
        }
    }
}
