package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public record ParticleEmitter(
        ParticleFactory factory,
        BlockParticleExpression chance,
        BlockParticleExpression count,
        BlockParticleExpression x,
        BlockParticleExpression y,
        BlockParticleExpression z,
        BlockParticleExpression dx,
        BlockParticleExpression dy,
        BlockParticleExpression dz
) {

    public static final Codec<ParticleEmitter> CODEC = RecordCodecBuilder.create(i -> i.group(
            ParticleFactory.CODEC.fieldOf("particle").forGetter(ParticleEmitter::factory),
            BlockParticleExpression.CODEC.fieldOf("chance").forGetter(ParticleEmitter::chance),
            StrOpt.of(BlockParticleExpression.CODEC, "count", BlockParticleExpression.ONE).forGetter(ParticleEmitter::count),
            BlockParticleExpression.CODEC.fieldOf("x").forGetter(ParticleEmitter::x),
            BlockParticleExpression.CODEC.fieldOf("y").forGetter(ParticleEmitter::y),
            BlockParticleExpression.CODEC.fieldOf("z").forGetter(ParticleEmitter::z),
            StrOpt.of(BlockParticleExpression.CODEC, "dx", BlockParticleExpression.ZERO).forGetter(ParticleEmitter::dx),
            StrOpt.of(BlockParticleExpression.CODEC, "dy", BlockParticleExpression.ZERO).forGetter(ParticleEmitter::dy),
            StrOpt.of(BlockParticleExpression.CODEC, "dz", BlockParticleExpression.ZERO).forGetter(ParticleEmitter::dz)
    ).apply(i, ParticleEmitter::new));


    public void tick(Level level, BlockPos pos, BlockState state) {
        double spawnChance = chance.getValue(level, pos, state);
        if (level.random.nextFloat() < spawnChance) {
            for (int i = 0; i < count.getValue(level, pos, state); i++) {
                factory.createParticle((ClientLevel)level,
                        pos.getX() + x.getValue(level, pos, state),
                        pos.getY() + y.getValue(level, pos, state),
                        pos.getZ() + z.getValue(level, pos, state),
                        dx.getValue(level, pos, state),
                        dy.getValue(level, pos, state),
                        dz.getValue(level, pos, state),
                        state
                );
            }
        }
    }


}
