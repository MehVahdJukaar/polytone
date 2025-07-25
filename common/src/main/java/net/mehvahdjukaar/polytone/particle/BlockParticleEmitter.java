package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.block.BlockClientTickable;
import net.mehvahdjukaar.polytone.block.BlockContextExpression;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.mehvahdjukaar.polytone.utils.codec.BiggerCodecs;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record BlockParticleEmitter(
        Optional<Holder<ParticleType<?>>> particleType,
        BlockContextExpression chance,
        BlockContextExpression count,
        BlockContextExpression x,
        BlockContextExpression y,
        BlockContextExpression z,
        BlockContextExpression dx,
        BlockContextExpression dy,
        BlockContextExpression dz,
        Optional<BlockContextExpression> r,
        Optional<BlockContextExpression> g,
        Optional<BlockContextExpression> b,
        Optional<BlockContextExpression> a,
        Optional<BlockContextExpression> roll,
        Optional<BlockContextExpression> size,
        Optional<BlockContextExpression> custom,
        RuleTest predicate,
        Optional<HolderSet<Biome>> biomes,
        SpawnLocation spawnLocation
) implements BlockClientTickable {

    public static final Codec<BlockParticleEmitter> CODEC = RecordCodecBuilder.create(i -> BiggerCodecs.group(i,
            CodecUtils.forwardAwareHolderByNameCodec(BuiltInRegistries.PARTICLE_TYPE).fieldOf("particle").forGetter(BlockParticleEmitter::particleType),
            StrOpt.of(BlockContextExpression.CODEC,"chance", BlockContextExpression.ONE).forGetter(BlockParticleEmitter::chance),
            StrOpt.of(BlockContextExpression.CODEC, "count", BlockContextExpression.ONE).forGetter(BlockParticleEmitter::count),
            StrOpt.of(BlockContextExpression.CODEC,"x", BlockContextExpression.PARTICLE_RAND).forGetter(BlockParticleEmitter::x),
            StrOpt.of(BlockContextExpression.CODEC,"y", BlockContextExpression.PARTICLE_RAND).forGetter(BlockParticleEmitter::y),
            StrOpt.of(BlockContextExpression.CODEC,"z", BlockContextExpression.PARTICLE_RAND).forGetter(BlockParticleEmitter::z),
            StrOpt.of(BlockContextExpression.CODEC,"dx", BlockContextExpression.ZERO).forGetter(BlockParticleEmitter::dx),
            StrOpt.of(BlockContextExpression.CODEC,"dy", BlockContextExpression.ZERO).forGetter(BlockParticleEmitter::dy),
            StrOpt.of(BlockContextExpression.CODEC,"dz", BlockContextExpression.ZERO).forGetter(BlockParticleEmitter::dz),
            BlockContextExpression.CODEC.optionalFieldOf("red").forGetter(BlockParticleEmitter::r),
            BlockContextExpression.CODEC.optionalFieldOf("green").forGetter(BlockParticleEmitter::g),
            BlockContextExpression.CODEC.optionalFieldOf("blue").forGetter(BlockParticleEmitter::b),
            BlockContextExpression.CODEC.optionalFieldOf("alpha").forGetter(BlockParticleEmitter::a),
            BlockContextExpression.CODEC.optionalFieldOf("roll").forGetter(BlockParticleEmitter::roll),
            BlockContextExpression.CODEC.optionalFieldOf("size").forGetter(BlockParticleEmitter::size),
            BlockContextExpression.CODEC.optionalFieldOf("custom").forGetter(BlockParticleEmitter::custom),
            CodecUtils.lenientWithLog(RuleTest.CODEC, "state_predicate", AlwaysTrueTest.INSTANCE).forGetter(BlockParticleEmitter::predicate),
            CodecUtils.forwardAwareHomogeneousList(Registries.BIOME).optionalFieldOf("biomes").forGetter(BlockParticleEmitter::biomes),
            StrOpt.of(SpawnLocation.CODEC,"spawn_location", SpawnLocation.CENTER).forGetter(BlockParticleEmitter::spawnLocation)
    ).apply(i, BlockParticleEmitter::new));

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (particleType.isEmpty()) return;
        double spawnChance = chance.getValue(level, pos, state);
        if (level.random.nextFloat() < spawnChance && predicate().test(state, level.random)) {

                if (biomes.isPresent()) {
                var biome = level.getBiome(pos);
                if (!biomes.get().contains(biome)) return;
            }
            for (int i = 0; i < count.getValue(level, pos, state); i++) {
                CustomParticleType.setStateHack(state);

                ParticleOptions po = getParticleOptions(level, pos, state);
                if (po == null) return;
                var pp = spawnLocation.getLocation(pos, state, level.random);
                level.addParticle(po,
                        pp.x() + x.getValue(level, pos, state),
                        pp.y() + y.getValue(level, pos, state),
                        pp.z() + z.getValue(level, pos, state),
                        dx.getValue(level, pos, state),
                        dy.getValue(level, pos, state),
                        dz.getValue(level, pos, state)
                );
            }
        }
    }

    private @Nullable ParticleOptions getParticleOptions(Level level, BlockPos pos, BlockState state) {
        ParticleOptions po;

        var particleTypeValue = particleType.get().value();

        if (Polytone.CUSTOM_PARTICLES.isDynamicParticle(particleType.get().unwrapKey().get().location())) {
            Map<String, Float> map = new HashMap<>();
            r.ifPresent(exp -> map.put("red", (float) exp.getValue(level, pos, state)));
            g.ifPresent(exp -> map.put("green", (float) exp.getValue(level, pos, state)));
            b.ifPresent(exp -> map.put("blue", (float) exp.getValue(level, pos, state)));
            a.ifPresent(exp -> map.put("alpha", (float) exp.getValue(level, pos, state)));
            roll.ifPresent(exp -> map.put("roll", (float) exp.getValue(level, pos, state)));
            size.ifPresent(exp -> map.put("size", (float) exp.getValue(level, pos, state)));
            custom.ifPresent(exp -> map.put("custom", (float) exp.getValue(level, pos, state)));
            return new ExtraDataParticleOptions(map, (ParticleType<ExtraDataParticleOptions>) particleTypeValue);
        }

        if (particleTypeValue instanceof SimpleParticleType st) {
            po = st;
        } else if (particleTypeValue == ParticleTypes.BLOCK || particleTypeValue == ParticleTypes.FALLING_DUST || particleTypeValue == ParticleTypes.BLOCK_MARKER || particleTypeValue == ParticleTypes.DUST) {
            po = new BlockParticleOption((ParticleType<BlockParticleOption>) particleTypeValue, state);
        } else if (particleTypeValue == ParticleTypes.ITEM) {
            po = new ItemParticleOption((ParticleType<ItemParticleOption>) particleTypeValue, state.getBlock().asItem().getDefaultInstance());
        } else {
            Polytone.LOGGER.error("Unsupported particle type: {}", particleTypeValue);
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
