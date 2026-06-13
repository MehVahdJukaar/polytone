package net.mehvahdjukaar.polytone.common.codec_ui.example;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaRecordBuilder;

/** Demo record exercising bool, float, double and int schemas. */
public record PhysicsConfigExample(boolean enabled, float speed, double mass, int priority) {

    public static final SchemaCodec<PhysicsConfigExample> SCHEMA_CODEC;

    static {
        SchemaCodec<Boolean> bool = SchemaCodec.of(Codec.BOOL, new Schema.Bool());
        SchemaCodec<Float> speed = SchemaCodec.of(Codec.floatRange(0f, 100f), new Schema.FloatRange(0f, 100f));
        SchemaCodec<Double> mass = SchemaCodec.of(Codec.doubleRange(0.0, 1000.0), new Schema.DoubleRange(0.0, 1000.0));
        SchemaCodec<Integer> prio = SchemaCodec.of(Codec.intRange(0, 10), Schema.intRange(0, 10));

        SchemaRecordBuilder<PhysicsConfigExample> b = SchemaRecordBuilder.of(PhysicsConfigExample.class);
        var fE = b.field("enabled", bool, PhysicsConfigExample::enabled);
        var fS = b.field("speed", speed, PhysicsConfigExample::speed);
        var fM = b.field("mass", mass, PhysicsConfigExample::mass);
        var fP = b.optional("priority", prio, 5, PhysicsConfigExample::priority);
        SCHEMA_CODEC = b.build4(PhysicsConfigExample::new, fE, fS, fM, fP);
    }
}
