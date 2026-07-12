package net.mehvahdjukaar.polytone.content.particle;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.TokenBucketTracker;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.content.block.BlockClientTickable;
import net.mehvahdjukaar.polytone.content.block.TickSource;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleInstance;
import net.mehvahdjukaar.polytone.content.particle.custom.ExtraDataParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
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
        IBlockExp chance,
        IBlockExp count,
        IBlockExp x,
        IBlockExp y,
        IBlockExp z,
        IBlockExp dx,
        IBlockExp dy,
        IBlockExp dz,
        Optional<IBlockExp> r,
        Optional<IBlockExp> g,
        Optional<IBlockExp> b,
        Optional<IBlockExp> a,
        Optional<IBlockExp> roll,
        Optional<IBlockExp> size,
        Optional<IBlockExp> custom,
        RuleTest predicate,
        Optional<HolderSet<Biome>> biomes,
        SpawnLocation spawnLocation,
        TickSource spawnSource
) implements BlockClientTickable {

    public static final SchemaCodec<BlockParticleEmitter> CODEC = SchemaRecord.create(BlockParticleEmitter.class, i -> i.group(
            i.field("particle", CodecUtils.forwardAwareHolderByNameCodec(BuiltInRegistries.PARTICLE_TYPE), BlockParticleEmitter::particleType),
            i.optional("chance", IBlockExp.CODEC, IBlockExp.ONE, BlockParticleEmitter::chance),
            i.optional("count", IBlockExp.CODEC, IBlockExp.ONE, BlockParticleEmitter::count),
            i.optional("x", IBlockExp.CODEC, IBlockExp.PARTICLE_RAND, BlockParticleEmitter::x),
            i.optional("y", IBlockExp.CODEC, IBlockExp.PARTICLE_RAND, BlockParticleEmitter::y),
            i.optional("z", IBlockExp.CODEC, IBlockExp.PARTICLE_RAND, BlockParticleEmitter::z),
            i.optional("dx", IBlockExp.CODEC, IBlockExp.ZERO, BlockParticleEmitter::dx),
            i.optional("dy", IBlockExp.CODEC, IBlockExp.ZERO, BlockParticleEmitter::dy),
            i.optional("dz", IBlockExp.CODEC, IBlockExp.ZERO, BlockParticleEmitter::dz),
            i.optional("red", IBlockExp.CODEC, BlockParticleEmitter::r),
            i.optional("green", IBlockExp.CODEC, BlockParticleEmitter::g),
            i.optional("blue", IBlockExp.CODEC, BlockParticleEmitter::b),
            i.optional("alpha", IBlockExp.CODEC, BlockParticleEmitter::a),
            i.optional("roll", IBlockExp.CODEC, BlockParticleEmitter::roll),
            i.optional("size", IBlockExp.CODEC, BlockParticleEmitter::size),
            i.optional("custom", IBlockExp.CODEC, BlockParticleEmitter::custom),
            i.field("state_predicate", SchemaCodecs.lenientWithLog(RuleTest.CODEC, "state_predicate", AlwaysTrueTest.INSTANCE), RuleTest.CODEC, BlockParticleEmitter::predicate),
            i.optional("biomes", CodecUtils.forwardAwareHomogeneousList(Registries.BIOME), BlockParticleEmitter::biomes),
            i.optional("spawn_location", SpawnLocation.CODEC, SpawnLocation.CENTER, BlockParticleEmitter::spawnLocation),
            i.optional("tick_source", TickSource.CODEC, TickSource.ANIMATE_TICK, BlockParticleEmitter::spawnSource)
    ).apply(i, BlockParticleEmitter::new));

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, TickSource source) {
        if (particleType.isEmpty()) {
            return;
        }
        if (source != spawnSource) {
            return; //only spawn particles on the correct tick source
        }
        float throttle = Polytone.CONFIGS.particlesThrottle.get();
        if (throttle < 1 && level.random.nextFloat() > throttle) return;

        Vec3 v = pos.getCenter();
        double spawnChance = chance.evaluate(level, v, state);
        if (level.random.nextFloat() < spawnChance && predicate().test(state, level.random)) {
            if (biomes.isPresent()) {
                var biome = level.getBiome(pos);
                if (!biomes.get().contains(biome)) return;
            }
            for (int i = 0; i < count.evaluate(level, v, state); i++) {
                CustomParticleInstance.setStateHack(state);

                ParticleOptions po = getParticleOptions(level, pos, state);
                if (po == null) return;
                if (!TokenBucketTracker.canEmitParticle(this)) return;
                var pp = spawnLocation.getLocation(pos, state, level.random);
                level.addAlwaysVisibleParticle(po,
                        pp.x() + x.evaluate(level, v, state),
                        pp.y() + y.evaluate(level, v, state),
                        pp.z() + z.evaluate(level, v, state),
                        dx.evaluate(level, v, state),
                        dy.evaluate(level, v, state),
                        dz.evaluate(level, v, state)
                );
            }
        }
    }

    private @Nullable ParticleOptions getParticleOptions(Level level, BlockPos pos, BlockState state) {
        ParticleOptions po;

        var particleTypeValue = particleType.get().value();

        if (Polytone.CUSTOM_PARTICLES.isDynamicParticle(particleType.get().unwrapKey().get().location())) {
            Map<String, Float> map = new HashMap<>();
            Vec3 v = pos.getCenter();
            r.ifPresent(exp -> map.put("red", (float) exp.evaluate(level, v, state)));
            g.ifPresent(exp -> map.put("green", (float) exp.evaluate(level, v, state)));
            b.ifPresent(exp -> map.put("blue", (float) exp.evaluate(level, v, state)));
            a.ifPresent(exp -> map.put("alpha", (float) exp.evaluate(level, v, state)));
            roll.ifPresent(exp -> map.put("roll", (float) exp.evaluate(level, v, state)));
            size.ifPresent(exp -> map.put("size", (float) exp.evaluate(level, v, state)));
            custom.ifPresent(exp -> map.put("custom", (float) exp.evaluate(level, v, state)));
            return new ExtraDataParticleOptions(map, particleTypeValue);
        }

        if (particleTypeValue instanceof SimpleParticleType st) {
            po = st;
        } else if (particleTypeValue == ParticleTypes.BLOCK || particleTypeValue == ParticleTypes.FALLING_DUST || particleTypeValue == ParticleTypes.BLOCK_MARKER || particleTypeValue == ParticleTypes.DUST_PILLAR) {
            po = new BlockParticleOption((ParticleType<BlockParticleOption>) particleTypeValue, state);
        } else if (particleTypeValue == ParticleTypes.ITEM) {
            po = new ItemParticleOption((ParticleType<ItemParticleOption>) particleTypeValue, state.getBlock().asItem().getDefaultInstance());
        } else {
            Polytone.LOGGER.error("Unsupported particle type: {}", particleTypeValue);
            return null;
        }
        return po;
    }

    public enum SpawnLocation implements StringRepresentable {
        CENTER, LOWER_CORNER, BLOCK_FACES;
        public static final Codec<SpawnLocation> CODEC = StringRepresentable.fromEnum(SpawnLocation::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

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
