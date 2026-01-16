package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.IParticleExp;
import net.minecraft.client.multiplayer.ClientLevel;

public interface ICustomParticleTicker {

    void tick(CustomParticleType.Instance particle, ClientLevel level);

    Codec<ICustomParticleTicker> CODEC = CodecUtils.alternatives(
            MultiExpressionParticleTicker.CODEC,
            IParticleExp.CODEC.xmap(e -> e::evaluate,
                    p -> IParticleExp.ZERO
            ));

    ICustomParticleTicker NO_OP = (particle, level) -> {
    };

}
