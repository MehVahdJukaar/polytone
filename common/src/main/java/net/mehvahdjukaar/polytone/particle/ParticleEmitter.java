package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public record ParticleEmitter(
        ParticleFactory factory,
        BlockParticleExpression chance,
        BlockParticleExpression count,
        BlockParticleExpression x,
        BlockParticleExpression y,
        BlockParticleExpression z,
        BlockParticleExpression dx,
        BlockParticleExpression dy,
        BlockParticleExpression dz,
        SpawnLocation spawnLocation
) {

    public static final Codec<ParticleEmitter> CODEC = RecordCodecBuilder.create(i -> i.group(
            ParticleFactory.CODEC.fieldOf("particle").forGetter(ParticleEmitter::factory),
            BlockParticleExpression.CODEC.fieldOf("chance").forGetter(ParticleEmitter::chance),
            BlockParticleExpression.CODEC.optionalFieldOf("count", BlockParticleExpression.ONE).forGetter(ParticleEmitter::count),
            BlockParticleExpression.CODEC.fieldOf("x").forGetter(ParticleEmitter::x),
            BlockParticleExpression.CODEC.fieldOf("y").forGetter(ParticleEmitter::y),
            BlockParticleExpression.CODEC.fieldOf("z").forGetter(ParticleEmitter::z),
            BlockParticleExpression.CODEC.optionalFieldOf("dx", BlockParticleExpression.ZERO).forGetter(ParticleEmitter::dx),
            BlockParticleExpression.CODEC.optionalFieldOf("dy", BlockParticleExpression.ZERO).forGetter(ParticleEmitter::dy),
            BlockParticleExpression.CODEC.optionalFieldOf("dz", BlockParticleExpression.ZERO).forGetter(ParticleEmitter::dz),
            SpawnLocation.CODEC.optionalFieldOf("spawn_location", SpawnLocation.CENTER).forGetter(ParticleEmitter::spawnLocation)
    ).apply(i, ParticleEmitter::new));


    public void tick(Level level, BlockPos pos, BlockState state) {
        double spawnChance = chance.getValue(level, pos, state);
        if (level.random.nextFloat() < spawnChance) {
            for (int i = 0; i < count.getValue(level, pos, state); i++) {
                factory.createParticle((ClientLevel) level,
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

    public enum SpawnLocation {
        CENTER, LOWER_CORNER, BLOCK_FACES;

        public static final Codec<SpawnLocation> CODEC = Codec.STRING.xmap(s -> SpawnLocation.valueOf(s.toUpperCase(Locale.ROOT)),
                e -> e.name().toLowerCase(Locale.ROOT));

        Vec3 getLocation(BlockPos pos, BlockState state, RandomSource rand) {
            return switch (this) {
                case LOWER_CORNER -> Vec3.atLowerCornerOf(pos);
                case CENTER -> Vec3.atCenterOf(pos);
                case BLOCK_FACES -> {
                    Direction dir = Direction.values()[rand.nextInt(Direction.values().length)];
                    yield getParticleSpawnPosOnFace(rand, pos, dir);
                }
            };
        }
    }


    public static Vec3 getParticleSpawnPosOnFace(RandomSource random, BlockPos pos, Direction direction) {
        Vec3 vec3 = Vec3.atCenterOf(pos);
        int i = direction.getStepX();
        int j = direction.getStepY();
        int k = direction.getStepZ();
        double d0 = vec3.x + (i == 0 ? Mth.nextDouble(random, -0.5D, 0.5D) : i * 0.6D);
        double d1 = vec3.y + (j == 0 ? Mth.nextDouble(random, -0.5D, 0.5D) : j * 0.6D);
        double d2 = vec3.z + (k == 0 ? Mth.nextDouble(random, -0.5D, 0.5D) : k * 0.6D);
        return new Vec3(d0, d1, d2);
    }

}
