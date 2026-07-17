package net.mehvahdjukaar.polytone.content.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;

public interface IPackMetadataExp {

    Codec<IPackMetadataExp> CODEC = Codec.lazyInitialized(() -> SchemaCodecs.labeled(
            SchemaCodecs.alternatives(
                    Codec.BOOL.xmap(
                            b -> (IPackMetadataExp) () -> b,
                            i -> false
                    ),
                    PackMetadataExp.TYPE.codec()
            ),
            SchemaCodecs.alt("constant", Codec.BOOL),
            SchemaCodecs.alt("expression", PackMetadataExp.TYPE.codec())));

    boolean evaluate();

    IPackMetadataExp FALSE = () -> false;
    IPackMetadataExp TRUE = () -> true;

}
