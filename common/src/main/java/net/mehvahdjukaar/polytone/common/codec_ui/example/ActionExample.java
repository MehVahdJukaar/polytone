package net.mehvahdjukaar.polytone.common.codec_ui.example;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaMapCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaRecordBuilder;

import java.util.Map;

/** Demo sum type dispatched on a "type" string. */
public sealed interface ActionExample {

    String typeKey();

    record Move(double x, double y, double z) implements ActionExample {
        public static final SchemaMapCodec<Move> MAP_CODEC;

        static {
            SchemaCodec<Double> coord = SchemaCodec.of(
                    Codec.doubleRange(-1000.0, 1000.0),
                    new Schema.DoubleRange(-1000.0, 1000.0)
            );
            SchemaRecordBuilder<Move> b = SchemaRecordBuilder.of(Move.class);
            var fX = b.field("x", coord, Move::x);
            var fY = b.field("y", coord, Move::y);
            var fZ = b.field("z", coord, Move::z);
            MAP_CODEC = b.buildMapCodec3(Move::new, fX, fY, fZ);
        }

        @Override
        public String typeKey() { return "move"; }
    }

    record Attack(int damage, boolean critical) implements ActionExample {
        public static final SchemaMapCodec<Attack> MAP_CODEC;

        static {
            SchemaCodec<Integer> damageCodec = SchemaCodec.of(Codec.intRange(0, 100), Schema.intRange(0, 100));
            SchemaCodec<Boolean> critCodec = SchemaCodec.of(Codec.BOOL, new Schema.Bool());
            SchemaRecordBuilder<Attack> b = SchemaRecordBuilder.of(Attack.class);
            var fD = b.field("damage", damageCodec, Attack::damage);
            var fC = b.optional("critical", critCodec, false, Attack::critical);
            MAP_CODEC = b.buildMapCodec2(Attack::new, fD, fC);
        }

        @Override
        public String typeKey() { return "attack"; }
    }

    record Speak(String message) implements ActionExample {
        public static final SchemaMapCodec<Speak> MAP_CODEC;

        static {
            SchemaCodec<String> stringCodec = SchemaCodec.wrap(Codec.STRING);
            SchemaRecordBuilder<Speak> b = SchemaRecordBuilder.of(Speak.class);
            var fM = b.field("message", stringCodec, Speak::message);
            MAP_CODEC = b.buildMapCodec1(Speak::new, fM);
        }

        @Override
        public String typeKey() { return "speak"; }
    }

    SchemaCodec<ActionExample> SCHEMA_CODEC = SchemaCodecs.dispatch(
            "type",
            ActionExample::typeKey,
            Map.of(
                    "move", Move.MAP_CODEC,
                    "attack", Attack.MAP_CODEC,
                    "speak", Speak.MAP_CODEC
            )
    );
}
