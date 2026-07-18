package net.mehvahdjukaar.polytone.content.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.minecraft.world.entity.Entity;

public interface IEntityExp {

    Codec<IEntityExp> CODEC = Codec.lazyInitialized(() -> SchemaCodecs.labeled(
            SchemaCodecs.alternatives(
                    CodecUtils.LENIENT_DOUBLE.xmap(
                            aDouble -> (IEntityExp) (e) -> aDouble,
                            i -> 0.0
                    ),
                    EntityExp.TYPE.codec()),
            // plain number for the picker; the wire codec above still accepts numeric strings via
            // LENIENT_DOUBLE. Labeling with LENIENT_DOUBLE would splice its union flat, leaking "number"/"text".
            SchemaCodecs.alt("constant", Codec.DOUBLE),
            SchemaCodecs.alt("expression", EntityExp.TYPE.codec()))
    );

    double evaluate(Entity entity);

    IEntityExp ZERO = (p) -> 0.0;
    IEntityExp ONE = (p) -> 1.0;
    IEntityExp PARTICLE_RAND = (a) -> (Math.random() * 2 - 1) * 0.4;

}
