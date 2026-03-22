package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;

public interface ICameraExp {

    Codec<ICameraExp> CODEC = Codec.lazyInitialized(() ->
            CodecUtils.alternatives(
                    Codec.DOUBLE.xmap(
                            aDouble -> () -> aDouble,
                            iBlockExp -> 0.0
                    ),
                    CameraExp.TYPE.codec())
    );

    double evaluate();

    ICameraExp ZERO = () -> 0.0;
    ICameraExp ONE = () -> 1.0;

}
