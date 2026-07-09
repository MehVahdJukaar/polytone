package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.exp.impl.ParticleContextExpression;
import net.mehvahdjukaar.polytone.common.expressions.ParticleExpEnv;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.Level;

public interface IParticleExp {

    Codec<IParticleExp> CODEC = Codec.lazyInitialized(() ->
            CodecUtils.alternatives(
                    CodecUtils.LENIENT_DOUBLE.xmap(
                            aDouble -> (level, pos) -> aDouble,
                            iBlockExp -> 0.0
                    ),
                    ParticleContextExpression.CODEC, ParticleExp.TYPE.codec())
    );

    double evaluate(Particle particle, Level level);

    /**
     * Evaluate using a reusable per-thread variable environment, avoiding a fresh var-map build per
     * call. Constants and the legacy exp4j path ignore the env and fall back to the plain overload.
     */
    default double evaluate(Particle particle, Level level, ParticleExpEnv env) {
        return evaluate(particle, level);
    }

    IParticleExp ZERO = (p, l) -> 0.0;
    IParticleExp ONE = (p, l) -> 1.0;
    IParticleExp PARTICLE_RAND = (a, b) -> (Math.random() * 2 - 1) * 0.4;

}