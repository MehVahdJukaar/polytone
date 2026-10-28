package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.expressions.impl.IParticleExp;
import net.minecraft.client.multiplayer.ClientLevel;

public interface ICustomParticleTicker {

    void tick(CustomParticleInstance particle, ClientLevel level);

    Codec<ICustomParticleTicker> CODEC = SchemaCodecs.alternatives(
            "multi", MultiExpressionParticleTicker.CODEC,
            "expression", IParticleExp.CODEC.xmap(e -> e::evaluate,
                    p -> IParticleExp.ZERO
            ));

    ICustomParticleTicker NO_OP = (particle, level) -> {
    };

}
