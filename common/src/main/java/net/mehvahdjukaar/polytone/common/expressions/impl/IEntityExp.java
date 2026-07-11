package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.exp.impl.EntityContextExpression;
import net.minecraft.world.entity.Entity;

public interface IEntityExp {

    Codec<IEntityExp> CONSTANT_CODEC = CodecUtils.LENIENT_DOUBLE.xmap(
            aDouble -> (e) -> aDouble,
            iBlockExp -> 0.0);

    Codec<IEntityExp> CODEC = Codec.lazyInitialized(() -> SchemaCodecs.alternatives(
            "constant", CONSTANT_CODEC,
            "legacy expression", EntityContextExpression.CODEC,
            "expression", EntityExp.TYPE.codec()));

    double evaluate(Entity entity);

    IEntityExp ZERO = (p) -> 0.0;
    IEntityExp ONE = (p) -> 1.0;
    IEntityExp PARTICLE_RAND = (a) -> (Math.random() * 2 - 1) * 0.4;

}