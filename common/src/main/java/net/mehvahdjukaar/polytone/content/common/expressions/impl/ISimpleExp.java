package net.mehvahdjukaar.polytone.content.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;

public interface ISimpleExp {

    Codec<ISimpleExp> CODEC = Codec.lazyInitialized(() -> SchemaCodecs.labeled(
            SchemaCodecs.alternatives(
                    CodecUtils.LENIENT_DOUBLE.xmap(
                            aDouble -> (ISimpleExp) () -> aDouble,
                            i -> 0.0
                    ),
                    SimpleExp.TYPE.codec()
            ),
            SchemaCodecs.alt("constant", CodecUtils.LENIENT_DOUBLE),
            SchemaCodecs.alt("expression", SimpleExp.TYPE.codec())));

    double evaluate();

    ISimpleExp ZERO = () -> 0.0;
    ISimpleExp ONE = () -> 1.0;

}
