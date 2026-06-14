package net.mehvahdjukaar.polytone.mixins.codec_ui;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaResolver;
import net.mehvahdjukaar.polytone.common.codec_ui.internal.FieldOfTags;
import net.mehvahdjukaar.polytone.common.codec_ui.internal.SchemaTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

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

    // fieldOf(name) -> MapCodec<A>. The output is an anonymous MapCodec.of(...) that wraps
    // `this` under a single named field. Tag it as a single-field Schema.Record so the
    // RecordCodecBuilder mixin (which calls resolveMap on this MapCodec when handling the
    // 2-arg of(getter, MapCodec) form) can unwrap it and inline the inner field type.
    @ModifyReturnValue(method = "fieldOf(Ljava/lang/String;)Lcom/mojang/serialization/MapCodec;",
            at = @At("RETURN"))
    private MapCodec<?> polytone$tagFieldOf(MapCodec<?> wrapped, @Local(argsOnly = true) String name) {
        polytone$tagSingleField(wrapped, name, false, null);
        return wrapped;
    }

    @ModifyReturnValue(
            method = "optionalFieldOf(Ljava/lang/String;Ljava/lang/Object;)Lcom/mojang/serialization/MapCodec;",
            at = @At("RETURN"))
    private MapCodec<?> polytone$tagOptionalFieldOfWithDefault(
            MapCodec<?> wrapped, @Local(argsOnly = true) String name, @Local(argsOnly = true) Object defaultValue) {
        polytone$tagSingleField(wrapped, name, true, defaultValue);
        return wrapped;
    }

    @ModifyReturnValue(method = "optionalFieldOf(Ljava/lang/String;)Lcom/mojang/serialization/MapCodec;",
            at = @At("RETURN"))
    private MapCodec<?> polytone$tagOptionalFieldOf(MapCodec<?> wrapped, @Local(argsOnly = true) String name) {
        polytone$tagSingleField(wrapped, name, true, null);
        return wrapped;
    }

    @Unique
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void polytone$tagSingleField(MapCodec<?> wrapped, String name, boolean optional, Object defaultValue) {
        if (wrapped == null) return;
        try {
            // Store a LAZY tag: just (name, innerCodec). The resolver computes the inner
            // schema fresh at lookup time, so a companion registered AFTER this fieldOf
            // mixin fires still wins. Required because MC bootstrap calls fieldOf during
            // early init (e.g. BlockState.CODEC.fieldOf in RandomBlockStateMatchTest).
            Codec<?> inner = (Codec<?>) (Object) this;
            FieldOfTags.put(wrapped, name, inner, optional, defaultValue);
        } catch (Throwable ignored) {
            // Best-effort.
        }
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
            // LAZY: just record (wrapped -> inner). Resolver resolves inner fresh at lookup time.
            // Previously we eagerly called SchemaResolver.resolve here, which captured stale
            // (empty) schemas during MC bootstrap before companions were registered.
            net.mehvahdjukaar.polytone.common.codec_ui.internal.XmapTags.putCodec(
                    wrapped, (Codec<?>) (Object) this);
        } catch (Throwable ignored) {
            // Best-effort.
        }
    }
}
