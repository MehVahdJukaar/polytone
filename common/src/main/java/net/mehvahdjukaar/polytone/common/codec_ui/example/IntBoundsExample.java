package net.mehvahdjukaar.polytone.common.codec_ui.example;

import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.minecraft.advancements.criterion.MinMaxBounds;

import java.util.List;

/**
 * MC game-type editor: edit a {@link MinMaxBounds.Ints} range.
 *
 * <p>Pattern: <b>companion</b>. Vanilla {@code MinMaxBounds.Ints.CODEC} is built from a
 * {@code RecordCodecBuilder} that exposes two optional fields, {@code min} and {@code max} (both
 * {@code int}), wrapped in an {@code Either&lt;Record, int&gt;} so callers can either supply an
 * object or a bare integer (= "exactly N"). We describe just the record shape here — feeding the
 * vanilla codec the object form works on both sides and lines up cleanly with the editor's
 * structural rendering.
 *
 * <p>Skipped: the bare-integer (point) variant of the vanilla codec. The editor doesn't model
 * {@code Either} of "Record | scalar" with the same primary type without additional plumbing,
 * and the record form is strictly more general.
 */
public final class IntBoundsExample {

    private IntBoundsExample() {}

    public static final SchemaCodec<MinMaxBounds.Ints> SCHEMA_CODEC;

    static {
        Schema.IntRange intSchema = Schema.intRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
        List<Schema.Field<MinMaxBounds.Ints, ?>> fields = List.of(
                new Schema.Field<>("min", intSchema, true, null),
                new Schema.Field<>("max", intSchema, true, null)
        );
        Schema<MinMaxBounds.Ints> schema = new Schema.Record<>(MinMaxBounds.Ints.class, fields);
        SCHEMA_CODEC = SchemaCodec.of(MinMaxBounds.Ints.CODEC, schema);
    }
}
