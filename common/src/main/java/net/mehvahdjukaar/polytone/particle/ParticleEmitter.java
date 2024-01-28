package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record ParticleEmitter(
        SimpleParticleType type,
        BlockParticleExpression chance,
        BlockParticleExpression x,
        BlockParticleExpression y,
        BlockParticleExpression z,
        BlockParticleExpression dx,
        BlockParticleExpression dy,
        BlockParticleExpression dz
) {

    public static final Codec<ParticleEmitter> CODEC = RecordCodecBuilder.create(i -> i.group(
            Registry.PARTICLE_TYPE.byNameCodec().fieldOf("particle")
                    .flatXmap(t -> t instanceof SimpleParticleType s ?
                                    DataResult.success(s) :
                                    DataResult.error( "Particle must be of SimpleParticleType class"),
                            DataResult::success)
                    .forGetter(ParticleEmitter::type),
            BlockParticleExpression.CODEC.fieldOf("chance").forGetter(ParticleEmitter::chance),
            BlockParticleExpression.CODEC.fieldOf("x").forGetter(ParticleEmitter::x),
            BlockParticleExpression.CODEC.fieldOf("y").forGetter(ParticleEmitter::y),
            BlockParticleExpression.CODEC.fieldOf("z").forGetter(ParticleEmitter::z),
            BlockParticleExpression.CODEC.fieldOf("dx").forGetter(ParticleEmitter::dx),
            BlockParticleExpression.CODEC.fieldOf("dy").forGetter(ParticleEmitter::dy),
            BlockParticleExpression.CODEC.fieldOf("dz").forGetter(ParticleEmitter::dz)
    ).apply(i, ParticleEmitter::new));


    public void tick(Level level, BlockPos pos, BlockState state) {
        double spawnChance = chance.getValue(level, pos, state);
        if (level.random.nextFloat() < spawnChance) {
            level.addParticle(type,
                    pos.getX() + x.getValue(level, pos, state),
                    pos.getY() + y.getValue(level, pos, state),
                    pos.getZ() + z.getValue(level, pos, state),
                    dx.getValue(level, pos, state),
                    dy.getValue(level, pos, state),
                    dz.getValue(level, pos, state)
            );
        }
    }


}
