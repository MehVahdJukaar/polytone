package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.IParticleExp;
import net.minecraft.client.multiplayer.ClientLevel;

public interface ICustomParticleTicker {

    void tick(CustomParticleInstance particle, ClientLevel level);

    Codec<ICustomParticleTicker> CODEC = SchemaCodecs.labeled(
            SchemaCodecs.alternatives(
                    MultiExpressionParticleTicker.CODEC,
                    IParticleExp.CODEC_LEGACY.xmap(e -> e::evaluate,
                            p -> IParticleExp.ZERO
                    )),
            SchemaCodecs.alt("multi", MultiExpressionParticleTicker.CODEC),
            SchemaCodecs.alt("expression", IParticleExp.CODEC_LEGACY));

    ICustomParticleTicker NO_OP = (particle, level) -> {
    };

}
