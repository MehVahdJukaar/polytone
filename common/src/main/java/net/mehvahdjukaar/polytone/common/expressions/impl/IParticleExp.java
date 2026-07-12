package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.expressions.ParticleExpEnv;
import net.mehvahdjukaar.polytone.content.particle.ParticleContextExpression;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.Level;

public interface IParticleExp {

    Codec<IParticleExp> CODEC = Codec.lazyInitialized(() ->
            CodecUtils.alternatives(
                    CodecUtils.LENIENT_DOUBLE.xmap(
                            aDouble -> (IParticleExp) (p, l) -> aDouble,
                            i -> 0.0
                    ),
                    ParticleContextExpression.CODEC.xmap(
                            pce -> (IParticleExp) (particle, level) -> pce.getValue(particle, level),
                            i -> ParticleContextExpression.ZERO
                    ),
                    ParticleExp.TYPE.codec())
    );

    double evaluate(Particle particle, Level level);

    /**
     * Evaluate using a reusable per-thread variable environment, avoiding a fresh var-map build per
     * call. Constants and the legacy exp4j path ignore the env and fall back to the plain overload.
     */
    default double evaluateAsync(Particle particle, Level level, ParticleExpEnv env) {
        return evaluate(particle, level);
    }

    IParticleExp ZERO = (p, l) -> 0.0;
    IParticleExp ONE = (p, l) -> 1.0;
    IParticleExp PARTICLE_RAND = (a, b) -> (Math.random() * 2 - 1) * 0.4;

}
