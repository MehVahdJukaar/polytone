package net.mehvahdjukaar.polytone.common.codec_ui.example;

import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.List;

/**
 * MC game-type editor: edit a {@link MobEffectInstance}.
 *
 * <p>Pattern: <b>companion</b>. Vanilla {@code MobEffectInstance.CODEC} is a
 * {@code RecordCodecBuilder} whose JSON shape is a flat object:
 * <pre>{@code
 * { "id": "minecraft:speed", "amplifier": 0, "duration": 600,
 *   "ambient": false, "show_particles": true, "show_icon": true,
 *   "hidden_effect": { ... } }
 * }</pre>
 * We hand-craft a {@link Schema.Record} mirroring that shape so the editor renders structurally.
 * The schema's {@code "id"} field is described as a {@link Schema.ResourceId} pointing at
 * {@link Registries#MOB_EFFECT} even though the codec underneath is a
 * {@code Codec<Holder<MobEffect>>} — both serialize as the same string on the wire, so this
 * works exactly like {@code SchemaCodecs.registryEntry(...)} does for other registry codecs.
 *
 * <p>Skipped fields:
 * <ul>
 *     <li>{@code hidden_effect}: it's a recursive {@code Optional<MobEffectInstance.Details>}
 *     and the schema language doesn't natively model recursion. The vanilla codec defaults it
 *     to absent when the JSON omits the key, so we just leave it out of the schema and the
 *     user can edit other fields freely without ever seeing it.</li>
 * </ul>
 */
public final class MobEffectInstanceExample {

    private MobEffectInstanceExample() {}

    public static final SchemaCodec<MobEffectInstance> SCHEMA_CODEC;

    static {
        Schema<?> idSchema = new Schema.ResourceId(Registries.MOB_EFFECT);
        Schema.IntRange amplifierSchema = Schema.intRange(0, 255);
        Schema.IntRange durationSchema = Schema.intRange(-1, Integer.MAX_VALUE);
        Schema.Bool boolSchema = Schema.bool();

        @SuppressWarnings({"unchecked", "rawtypes"})
        List<Schema.Field<MobEffectInstance, ?>> fields = List.of(
                new Schema.Field("id", idSchema, false, null),
                new Schema.Field<>("amplifier", amplifierSchema, true, 0),
                new Schema.Field<>("duration", durationSchema, true, 0),
                new Schema.Field<>("ambient", boolSchema, true, false),
                new Schema.Field<>("show_particles", boolSchema, true, true),
                new Schema.Field<>("show_icon", boolSchema, true, true)
        );
        Schema<MobEffectInstance> schema = new Schema.Record<>(MobEffectInstance.class, fields);
        SCHEMA_CODEC = SchemaCodec.of(MobEffectInstance.CODEC, schema);
    }
}
