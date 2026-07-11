package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;

public interface IPackMetadataExp {

    Codec<IPackMetadataExp> CONSTANT_CODEC = Codec.BOOL.xmap(
            b -> () -> b,
            iBlockExp -> false);

    Codec<IPackMetadataExp> CODEC = Codec.lazyInitialized(() -> SchemaCodecs.alternatives(
            "constant", CONSTANT_CODEC,
            "expression", PackMetadataExp.TYPE.codec()));

    boolean evaluate();

    IPackMetadataExp FALSE = () -> false;
    IPackMetadataExp TRUE = () -> true;

}
