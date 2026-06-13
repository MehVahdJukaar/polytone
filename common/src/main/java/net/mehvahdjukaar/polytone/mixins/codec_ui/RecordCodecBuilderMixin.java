package net.mehvahdjukaar.polytone.mixins.codec_ui;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaResolver;
import net.mehvahdjukaar.polytone.common.codec_ui.internal.RecordFieldTags;
import net.mehvahdjukaar.polytone.common.codec_ui.internal.SchemaTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/**
 * Captures per-field information on {@link RecordCodecBuilder} during construction so the
 * MapCodec returned by {@code RecordCodecBuilder.build(...)} can carry a real
 * {@link Schema.Record}. Without this mixin, the anonymous {@code MapCodec} produced by
 * {@code build} is fully opaque to the {@link SchemaResolver}.
 *
 * <p>Coverage limitations:
 * <ul>
 *   <li>Only the {@code of(getter, name, codec)} and {@code of(getter, MapCodec)} entry
 *       points are tagged. RCBs created via {@code dependent(...)} or {@code point/stable/deprecated}
 *       carry empty tag lists; their fields will fall through to Opaque.</li>
 *   <li>The {@code Instance.lift1 / Instance.map} fast-paths are not tagged. In practice
 *       all real vanilla and mod record codecs go through {@code ap2/ap3/ap4} which we cover.</li>
 *   <li>Field optionality is best-effort: only the {@code of(getter, MapCodec)} form carries
 *       it (resolver introspects the MapCodec via the existing tier-2 OptionalFieldCodec path).
 *       The 3-arg {@code of(getter, name, codec)} marks fields as required.</li>
 * </ul>
 */
@Mixin(RecordCodecBuilder.class)
public abstract class RecordCodecBuilderMixin {

    @ModifyReturnValue(
            method = "of(Ljava/util/function/Function;Ljava/lang/String;Lcom/mojang/serialization/Codec;)Lcom/mojang/serialization/codecs/RecordCodecBuilder;",
            at = @At("RETURN"))
    private static RecordCodecBuilder<?, ?> polytone$tagOfNamed(
            RecordCodecBuilder<?, ?> result,
            @Local(argsOnly = true) String name,
            @Local(argsOnly = true) Codec<?> fieldCodec) {
        RecordFieldTags.single(result, name, fieldCodec);
        return result;
    }

    @ModifyReturnValue(
            method = "of(Ljava/util/function/Function;Lcom/mojang/serialization/MapCodec;)Lcom/mojang/serialization/codecs/RecordCodecBuilder;",
            at = @At("RETURN"))
    private static RecordCodecBuilder<?, ?> polytone$tagOfMap(
            RecordCodecBuilder<?, ?> result,
            @Local(argsOnly = true) MapCodec<?> mapCodec) {
        RecordFieldTags.singleMap(result, mapCodec);
        return result;
    }

    /**
     * On build, synthesise a Schema.Record from the accumulated field tags and attach it to
     * the produced MapCodec. The Class<O> is unknown at this point — pass {@code Object.class}
     * since the widget layer doesn't rely on it.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @ModifyReturnValue(method = "build", at = @At("RETURN"))
    private static MapCodec<?> polytone$tagBuild(
            MapCodec<?> result,
            @Local(argsOnly = true) App<?, ?> builderBox) {
        try {
            RecordCodecBuilder<?, ?> builder = RecordCodecBuilder.unbox((App) builderBox);
            List<RecordFieldTags.Entry> entries = RecordFieldTags.get(builder);
            if (entries.isEmpty()) return result;

            List<Schema.Field<?, ?>> fields = new ArrayList<>(entries.size());
            SchemaResolver resolver = SchemaResolver.get();
            for (RecordFieldTags.Entry e : entries) {
                Schema<?> fieldSchema;
                boolean optional;
                if (e.mapCodec() != null) {
                    // Delegate to MapCodec resolution; if it returns a single-field Record
                    // (OptionalFieldCodec path), unwrap to its inner schema + optionality.
                    Schema<?> mapSchema = resolver.resolveMap((MapCodec) e.mapCodec());
                    if (mapSchema instanceof Schema.Record<?> rec && rec.fields().size() == 1) {
                        Schema.Field<?, ?> inner = rec.fields().get(0);
                        fieldSchema = inner.schema();
                        optional = inner.optional();
                    } else {
                        fieldSchema = mapSchema;
                        optional = false;
                    }
                } else {
                    fieldSchema = resolver.resolve((Codec) e.elementCodec());
                    optional = false;
                }
                fields.add(new Schema.Field(e.name(), fieldSchema, optional, null));
            }
            Schema.Record schema = new Schema.Record(Object.class, List.copyOf(fields));
            SchemaTags.tag((MapCodec) result, (Schema) schema);
        } catch (Throwable ignored) {
            // Best-effort.
        }
        return result;
    }
}
