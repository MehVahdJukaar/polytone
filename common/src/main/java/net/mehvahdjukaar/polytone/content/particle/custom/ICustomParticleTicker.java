package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.IParticleExp;
import net.minecraft.client.multiplayer.ClientLevel;

public interface ICustomParticleTicker {

    void tick(CustomParticleInstance particle, ClientLevel level);

    // Labels resolve against the un-xmapped IParticleExp.CODEC so the branch keeps its schema
    // on NeoForge too (owned xmaps degrade to raw JSON there); its AnyOf splices flat.
    Codec<ICustomParticleTicker> CODEC = SchemaCodecs.labeled(
            SchemaCodecs.alternatives(
                    MultiExpressionParticleTicker.CODEC,
                    IParticleExp.CODEC.xmap(e -> e::evaluate,
                            p -> IParticleExp.ZERO
                    )),
            SchemaCodecs.alt("multi", MultiExpressionParticleTicker.CODEC),
            SchemaCodecs.alt("expression", IParticleExp.CODEC));

    ICustomParticleTicker NO_OP = (particle, level) -> {
    };

}
