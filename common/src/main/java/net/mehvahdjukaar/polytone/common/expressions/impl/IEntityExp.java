package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.minecraft.world.entity.Entity;

public interface IEntityExp {

    Codec<IEntityExp> CODEC = Codec.lazyInitialized(() ->
            CodecUtils.alternatives(
                    Codec.DOUBLE.xmap(
                            aDouble -> (IEntityExp) (e) -> aDouble,
                            i -> 0.0
                    ),
                    EntityExp.TYPE.codec())
    );

    double evaluate(Entity entity);

    IEntityExp ZERO = (p) -> 0.0;
    IEntityExp ONE = (p) -> 1.0;
    IEntityExp PARTICLE_RAND = (a) -> (Math.random() * 2 - 1) * 0.4;

}
