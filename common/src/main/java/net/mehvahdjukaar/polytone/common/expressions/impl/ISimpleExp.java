package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;

public interface ISimpleExp {

    Codec<ISimpleExp> CONSTANT_CODEC = Codec.DOUBLE.xmap(
            aDouble -> () -> aDouble,
            iBlockExp -> 0.0);

    // Wire codec + editor picker labels declared once each.
    Codec<ISimpleExp> CODEC = Codec.lazyInitialized(() -> SchemaCodecs.alternatives(
            "constant", CONSTANT_CODEC,
            "expression", SimpleExp.TYPE.codec()));

    double evaluate();

    ISimpleExp ZERO = () -> 0.0;
    ISimpleExp ONE = () -> 1.0;

}
