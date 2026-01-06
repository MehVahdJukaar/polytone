package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;

public interface ISimpleExp {

    @SuppressWarnings("unchecked")
    Codec<ISimpleExp> CODEC = Codec.lazyInitialized(() -> (
            (Codec) SimpleExp.CODEC));

    double evaluate();

    ISimpleExp ZERO = () -> 0.0;
    ISimpleExp ONE = () -> 1.0;

}
