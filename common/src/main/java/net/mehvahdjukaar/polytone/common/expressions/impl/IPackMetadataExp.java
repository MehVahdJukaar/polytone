package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;

public interface IPackMetadataExp {

    Codec<IPackMetadataExp> CODEC = Codec.lazyInitialized(() -> (
            CodecUtils.alternatives(
                    Codec.BOOL.xmap(
                            b -> (IPackMetadataExp) () -> b,
                            i -> false
                    ),
                    PackMetadataExp.TYPE.codec()
            )));

    boolean evaluate();

    IPackMetadataExp FALSE = () -> false;
    IPackMetadataExp TRUE = () -> true;

}
