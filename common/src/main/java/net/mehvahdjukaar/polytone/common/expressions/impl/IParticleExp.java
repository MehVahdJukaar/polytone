package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.exp.impl.ParticleContextExpression;
import net.mehvahdjukaar.polytone.common.expressions.ParticleExpEnv;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;

public interface IParticleExp {

    Codec<IParticleExp> CODEC = Codec.lazyInitialized(() -> SchemaCodecs.labeled(
            SchemaCodecs.alternatives(
                    CodecUtils.LENIENT_DOUBLE.xmap(
                            aDouble -> (IParticleExp) (p, l) -> aDouble,
                            i -> 0.0
                    ),
                    ParticleContextExpression.CODEC,
                    ParticleExp.TYPE.codec()),
            // constant: plain number (LENIENT_DOUBLE would splice its double-or-string union into
            // stray "number"/"text" options). expression before legacy: both encode as bare strings,
            // so fit-scoring on load should land on the modern branch, not the deprecated one.
            SchemaCodecs.alt("constant", Codec.DOUBLE),
            SchemaCodecs.alt("expression", ParticleExp.TYPE.codec()),
            SchemaCodecs.alt("legacy expression", ParticleContextExpression.CODEC)));

    double evaluate(Particle particle, ClientLevel level);

    /**
     * Evaluate using a reusable per-thread variable environment, avoiding a fresh var-map build per
     * call. Constants and the legacy exp4j path ignore the env and fall back to the plain overload.
     */
    default double evaluateAsync(Particle particle, ClientLevel level, ParticleExpEnv env) {
        return evaluate(particle, level);
    }

    IParticleExp ZERO = (p, l) -> 0.0;
    IParticleExp ONE = (p, l) -> 1.0;
    IParticleExp PARTICLE_RAND = (a, b) -> (Math.random() * 2 - 1) * 0.4;

}