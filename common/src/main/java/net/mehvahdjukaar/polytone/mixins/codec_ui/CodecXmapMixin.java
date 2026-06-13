package net.mehvahdjukaar.polytone.mixins.codec_ui;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaResolver;
import net.mehvahdjukaar.polytone.common.codec_ui.internal.SchemaTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Tags codecs returned by Codec's combinator default methods (xmap, flatXmap, ...) with the
 * schema of the inner codec ({@code this}). Most of these combinators do not change the
 * editable shape of the data — they're newtype wrappers, validation passes, or default
 * suppliers — so the inner schema is the correct shape for the GUI.
 *
 * <p>For combinators that <em>could</em> change shape semantically (e.g. an xmap that
 * encodes a tree as a string), the result is "harmlessly correct": the UI shows the
 * inner shape, which is what the on-disk JSON looks like.</p>
 *
 * <p>The unchecked cast of the inner Schema to {@code Schema<S>} is structurally safe
 * because the codec contract guarantees both representations parse the same JSON shape.</p>
 */
@Mixin(Codec.class)
public interface CodecXmapMixin {

    @ModifyReturnValue(method = "xmap", at = @At("RETURN"))
    private <S> Codec<S> polytone$tagXmap(Codec<S> wrapped) {
        polytone$inheritInner(wrapped);
        return wrapped;
    }

    @ModifyReturnValue(method = "flatXmap", at = @At("RETURN"))
    private <S> Codec<S> polytone$tagFlatXmap(Codec<S> wrapped) {
        polytone$inheritInner(wrapped);
        return wrapped;
    }

    @ModifyReturnValue(method = "comapFlatMap", at = @At("RETURN"))
    private <S> Codec<S> polytone$tagComapFlatMap(Codec<S> wrapped) {
        polytone$inheritInner(wrapped);
        return wrapped;
    }

    @ModifyReturnValue(method = "flatComapMap", at = @At("RETURN"))
    private <S> Codec<S> polytone$tagFlatComapMap(Codec<S> wrapped) {
        polytone$inheritInner(wrapped);
        return wrapped;
    }

    // Codec<A> validate(...) -> Codec<A>. Wrapper preserves A's schema.
    @ModifyReturnValue(method = "validate", at = @At("RETURN"))
    private Codec<?> polytone$tagValidate(Codec<?> wrapped) {
        polytone$inheritInner(wrapped);
        return wrapped;
    }

    @ModifyReturnValue(method = "mapResult", at = @At("RETURN"))
    private Codec<?> polytone$tagMapResult(Codec<?> wrapped) {
        polytone$inheritInner(wrapped);
        return wrapped;
    }

    // orElse(A) -> Codec<A>. Schema unchanged.
    @ModifyReturnValue(method = "orElse(Ljava/lang/Object;)Lcom/mojang/serialization/Codec;",
            at = @At("RETURN"))
    private Codec<?> polytone$tagOrElseValue(Codec<?> wrapped) {
        polytone$inheritInner(wrapped);
        return wrapped;
    }

    @ModifyReturnValue(method = "orElseGet(Ljava/util/function/Supplier;)Lcom/mojang/serialization/Codec;",
            at = @At("RETURN"))
    private Codec<?> polytone$tagOrElseGetSupplier(Codec<?> wrapped) {
        polytone$inheritInner(wrapped);
        return wrapped;
    }

    @ModifyReturnValue(method = "promotePartial(Ljava/util/function/Consumer;)Lcom/mojang/serialization/Codec;",
            at = @At("RETURN"))
    private Codec<?> polytone$tagPromotePartial(Codec<?> wrapped) {
        polytone$inheritInner(wrapped);
        return wrapped;
    }

    @ModifyReturnValue(method = "withLifecycle(Lcom/mojang/serialization/Lifecycle;)Lcom/mojang/serialization/Codec;",
            at = @At("RETURN"))
    private Codec<?> polytone$tagWithLifecycle(Codec<?> wrapped) {
        polytone$inheritInner(wrapped);
        return wrapped;
    }

    @ModifyReturnValue(method = "stable()Lcom/mojang/serialization/Codec;", at = @At("RETURN"))
    private Codec<?> polytone$tagStable(Codec<?> wrapped) {
        polytone$inheritInner(wrapped);
        return wrapped;
    }

    @ModifyReturnValue(method = "deprecated(I)Lcom/mojang/serialization/Codec;", at = @At("RETURN"))
    private Codec<?> polytone$tagDeprecated(Codec<?> wrapped) {
        polytone$inheritInner(wrapped);
        return wrapped;
    }

    /**
     * Resolves the inner codec's schema (via {@link SchemaTags} side-channel if previously
     * tagged, otherwise the full resolver) and tags the wrapper with it. We perform the
     * unchecked cross-cast since combinators preserve the on-disk JSON shape.
     */
    @Unique
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void polytone$inheritInner(Codec<?> wrapped) {
        if (wrapped == null || wrapped == (Object) this) return;
        try {
            Codec<Object> inner = (Codec<Object>) this;
            Schema<Object> innerSchema = SchemaTags.lookup(inner);
            if (innerSchema == null) {
                innerSchema = SchemaResolver.get().resolve(inner);
            }
            SchemaTags.tag((Codec) wrapped, (Schema) innerSchema);
        } catch (Throwable ignored) {
            // Best-effort; never propagate exceptions out of a constructor path.
        }
    }
}
