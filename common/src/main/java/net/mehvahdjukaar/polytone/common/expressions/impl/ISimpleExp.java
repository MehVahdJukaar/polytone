package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;

public interface ISimpleExp {

    Codec<ISimpleExp> CODEC = Codec.lazyInitialized(() -> SchemaCodecs.labeled(
            SchemaCodecs.alternatives(
                    CodecUtils.LENIENT_DOUBLE.xmap(
                            aDouble -> (ISimpleExp) () -> aDouble,
                            i -> 0.0
                    ),
                    SimpleExp.TYPE.codec()
            ),
            // plain number for the picker; the wire codec above still accepts numeric strings via
            // LENIENT_DOUBLE. Labeling with LENIENT_DOUBLE would splice its float-or-string union flat
            // and leak unlabeled "number"/"text" options.
            SchemaCodecs.alt("constant", Codec.DOUBLE),
            SchemaCodecs.alt("expression", SimpleExp.TYPE.codec())));

    double evaluate();

    ISimpleExp ZERO = () -> 0.0;
    ISimpleExp ONE = () -> 1.0;

}
