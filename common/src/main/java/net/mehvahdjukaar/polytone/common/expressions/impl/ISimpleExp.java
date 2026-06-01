package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;

public interface ISimpleExp {

    Codec<ISimpleExp> CODEC = Codec.lazyInitialized(() -> (
            CodecUtils.alternatives(
                    Codec.DOUBLE.xmap(
                            aDouble -> (ISimpleExp) () -> aDouble,
                            i -> 0.0
                    ),
                    SimpleExp.TYPE.codec()
            )));

    double evaluate();

    ISimpleExp ZERO = () -> 0.0;
    ISimpleExp ONE = () -> 1.0;

}
