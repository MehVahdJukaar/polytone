package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.block.BlockClientTickable;
import net.mehvahdjukaar.polytone.block.BlockContextExpression;
import net.mehvahdjukaar.polytone.utils.LazyHolderSet;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

public record BlockParticleEmitter(
        ParticleType<?> particleType,
        BlockContextExpression chance,
        BlockContextExpression count,
        BlockContextExpression x,
        BlockContextExpression y,
        BlockContextExpression z,
        BlockContextExpression dx,
        BlockContextExpression dy,
        BlockContextExpression dz,
        Optional<LazyHolderSet<Biome>> biomes,
        SpawnLocation spawnLocation
) implements BlockClientTickable {

    public static final Codec<BlockParticleEmitter> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.PARTICLE_TYPE.byNameCodec().fieldOf("particle").forGetter(BlockParticleEmitter::particleType),
            StrOpt.of(BlockContextExpression.CODEC,"chance", BlockContextExpression.ONE).forGetter(BlockParticleEmitter::chance),
            StrOpt.of(BlockContextExpression.CODEC, "count", BlockContextExpression.ONE).forGetter(BlockParticleEmitter::count),
            StrOpt.of(BlockContextExpression.CODEC,"x", BlockContextExpression.PARTICLE_RAND).forGetter(BlockParticleEmitter::x),
            StrOpt.of(BlockContextExpression.CODEC,"y", BlockContextExpression.PARTICLE_RAND).forGetter(BlockParticleEmitter::y),
            StrOpt.of(BlockContextExpression.CODEC,"z", BlockContextExpression.PARTICLE_RAND).forGetter(BlockParticleEmitter::z),
            StrOpt.of(BlockContextExpression.CODEC,"dx", BlockContextExpression.ZERO).forGetter(BlockParticleEmitter::dx),
            StrOpt.of(BlockContextExpression.CODEC,"dy", BlockContextExpression.ZERO).forGetter(BlockParticleEmitter::dy),
            StrOpt.of(BlockContextExpression.CODEC,"dz", BlockContextExpression.ZERO).forGetter(BlockParticleEmitter::dz),
            StrOpt.of(LazyHolderSet.codec(Registries.BIOME),"biomes").forGetter(BlockParticleEmitter::biomes),
            StrOpt.of(SpawnLocation.CODEC,"spawn_location", SpawnLocation.CENTER).forGetter(BlockParticleEmitter::spawnLocation)
    ).apply(i, BlockParticleEmitter::new));


    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        double spawnChance = chance.getValue(level, pos, state);
        if (level.random.nextFloat() < spawnChance) {
            if (biomes.isPresent()) {
                var biome = level.getBiome(pos);
                if (!biomes.get().contains(biome)) return;
            }
            for (int i = 0; i < count.getValue(level, pos, state); i++) {
                CustomParticleType.setStateHack(state);

                ParticleOptions po = getParticleOptions(state);
                if (po == null) return;
                level.addParticle(po,
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

    private @Nullable ParticleOptions getParticleOptions(BlockState state) {
        ParticleOptions po;

        if (particleType instanceof SimpleParticleType st) {
            po = st;
        } else if (particleType == ParticleTypes.BLOCK || particleType == ParticleTypes.FALLING_DUST || particleType == ParticleTypes.BLOCK_MARKER) {
            po = new BlockParticleOption((ParticleType<BlockParticleOption>) particleType, state);
        } else if (particleType == ParticleTypes.ITEM) {
            po = new ItemParticleOption((ParticleType<ItemParticleOption>) particleType, state.getBlock().asItem().getDefaultInstance());
        } else {
            Polytone.LOGGER.error("Unsupported particle type: {}", particleType);
            return null;
        }
        return po;
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
