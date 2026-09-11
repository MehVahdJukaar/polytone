package net.mehvahdjukaar.polytone.content.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.content.particle.ParticleContextExpression;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.Level;

public interface IParticleExp {

    Codec<IParticleExp> CODEC_LEGACY = Codec.lazyInitialized(() -> SchemaCodecs.labeled(
            SchemaCodecs.alternatives(
                    CodecUtils.LENIENT_DOUBLE.xmap(
                            aDouble ->  (p, l) -> aDouble,
                            i -> 0.0
                    ),
                    ParticleContextExpression.CODEC.xmap(
                            pce ->  (particle, level) -> pce.getValue(particle, level),
                            i -> ParticleContextExpression.ZERO
                    ),
                    ParticleExp.TYPE.codec()),
            SchemaCodecs.alt("constant", Codec.DOUBLE),
            SchemaCodecs.alt("expression", ParticleExp.TYPE.codec()),
            SchemaCodecs.alt("legacy expression", ParticleContextExpression.CODEC))
    );

    Codec<IParticleExp> CODEC = Codec.lazyInitialized(() -> SchemaCodecs.labeled(
            SchemaCodecs.alternatives(
                    CodecUtils.LENIENT_DOUBLE.xmap(aDouble -> (p, l) -> aDouble, i -> 0.0),
                    ParticleExp.TYPE.codec()),
            SchemaCodecs.alt("constant", Codec.DOUBLE),
            SchemaCodecs.alt("expression", ParticleExp.TYPE.codec())));

    double evaluate(Particle particle, Level level);

    IParticleExp ZERO = (p, l) -> 0.0;
    IParticleExp ONE = (p, l) -> 1.0;
    IParticleExp PARTICLE_RAND = (a, b) -> (Math.random() * 2 - 1) * 0.4;

}
