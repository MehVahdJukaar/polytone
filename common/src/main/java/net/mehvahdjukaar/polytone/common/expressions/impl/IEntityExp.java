package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.exp.impl.EntityContextExpression;
import net.mehvahdjukaar.polytone.common.exp.impl.ParticleContextExpression;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public interface IEntityExp {

    Codec<IEntityExp> CODEC = Codec.lazyInitialized(() ->
            CodecUtils.withAlternative(EntityContextExpression.CODEC, ParticleExp.TYPE.codec())
    );

    double evaluate(Entity entity);

    IEntityExp ZERO = (p) -> 0.0;
    IEntityExp ONE = (p) -> 1.0;
    IEntityExp PARTICLE_RAND = (a) -> (Math.random() * 2 - 1) * 0.4;

}