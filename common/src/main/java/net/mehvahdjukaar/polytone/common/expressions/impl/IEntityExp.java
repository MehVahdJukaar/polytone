package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.exp.impl.EntityContextExpression;
import net.minecraft.world.entity.Entity;

public interface IEntityExp {

    Codec<IEntityExp> CODEC = Codec.lazyInitialized(() -> SchemaCodecs.labeled(
            SchemaCodecs.alternatives(
                    CodecUtils.LENIENT_DOUBLE.xmap(
                            aDouble -> (IEntityExp) (e) -> aDouble,
                            i -> 0.0
                    ),
                    EntityContextExpression.CODEC,
                    EntityExp.TYPE.codec()),
            // constant: plain number (LENIENT_DOUBLE would splice its double-or-string union into
            // stray "number"/"text" options). expression before legacy: both encode as bare strings,
            // so fit-scoring on load should land on the modern branch, not the deprecated one.
            SchemaCodecs.alt("constant", Codec.DOUBLE),
            SchemaCodecs.alt("expression", EntityExp.TYPE.codec()),
            SchemaCodecs.alt("legacy expression", EntityContextExpression.CODEC)));

    double evaluate(Entity entity);

    IEntityExp ZERO = (p) -> 0.0;
    IEntityExp ONE = (p) -> 1.0;
    IEntityExp PARTICLE_RAND = (a) -> (Math.random() * 2 - 1) * 0.4;

}