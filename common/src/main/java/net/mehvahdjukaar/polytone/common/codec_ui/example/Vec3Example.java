package net.mehvahdjukaar.polytone.common.codec_ui.example;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaRecord;
import net.minecraft.world.phys.Vec3;

/**
 * MC game-type editor: edit a {@link Vec3}.
 *
 * <p>Pattern: <b>wrapper</b>. Vanilla {@code Vec3.CODEC} is built on
 * {@code Codec.DOUBLE.listOf()} and encodes as a 3-element JSON array {@code [x, y, z]}, which
 * the editor cannot render as named fields. We therefore expose a wrapper record with three
 * separately-editable {@code double} fields. The resulting JSON shape ({@code {x,y,z}}) does NOT
 * match the vanilla codec, but the wrapper's codec still ends up producing a real {@link Vec3}
 * on decode (via the {@code Vec3::new} constructor reference).
 */
public record Vec3Example(double x, double y, double z) {

    /** Convenience: convert this wrapper to a real {@link Vec3}. */
    public Vec3 toVec3() {
        return new Vec3(x, y, z);
    }

    /** Convenience: read a {@link Vec3} into a wrapper instance. */
    public static Vec3Example of(Vec3 v) {
        return new Vec3Example(v.x(), v.y(), v.z());
    }

    public static final SchemaCodec<Vec3Example> SCHEMA_CODEC = SchemaRecord.create(
            Vec3Example.class, i -> i.group(
                    i.field("x", Codec.DOUBLE, Vec3Example::x),
                    i.field("y", Codec.DOUBLE, Vec3Example::y),
                    i.field("z", Codec.DOUBLE, Vec3Example::z)
            ).apply(i, Vec3Example::new));
}
