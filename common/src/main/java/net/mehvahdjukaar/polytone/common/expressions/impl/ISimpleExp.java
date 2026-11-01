package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.expressions.PolyExpType;

public interface ISimpleExp {

    Codec<ISimpleExp> CODEC = Codec.lazyInitialized(() -> codec(SimpleExp.TYPE));
    Codec<ISimpleExp> CODEC_WITH_VALUE = Codec.lazyInitialized(() -> codec(SimpleExp.TYPE_WITH_VALUE));

    private static Codec<ISimpleExp> codec(PolyExpType<SimpleExp> type) {
        return SchemaCodecs.labeled(
                SchemaCodecs.alternatives(
                        CodecUtils.LENIENT_DOUBLE.xmap(
                                aDouble -> (ISimpleExp) () -> aDouble,
                                i -> 0.0
                        ),
                        type.codec()
                ),
                SchemaCodecs.alt("constant", Codec.DOUBLE),
                SchemaCodecs.alt("expression", type.codec()));
    }

    double evaluate();

    default double evaluate(double v) {
        return evaluate();
    }

    ISimpleExp ZERO = () -> 0.0;
    ISimpleExp ONE = () -> 1.0;

}
