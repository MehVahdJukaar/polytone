package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.exp.impl.ParticleContextExpression;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.Level;

public interface IParticleExp {

    Codec<IParticleExp> CONSTANT_CODEC = Codec.DOUBLE.xmap(
            aDouble -> (level, pos) -> aDouble,
            iBlockExp -> 0.0);

    Codec<IParticleExp> CODEC = Codec.lazyInitialized(() -> SchemaCodecs.alternatives(
            "constant", CONSTANT_CODEC,
            "legacy expression", ParticleContextExpression.CODEC,
            "expression", ParticleExp.TYPE.codec()));

    double evaluate(Particle particle, Level level);

    IParticleExp ZERO = (p, l) -> 0.0;
    IParticleExp ONE = (p, l) -> 1.0;
    IParticleExp PARTICLE_RAND = (a, b) -> (Math.random() * 2 - 1) * 0.4;

}