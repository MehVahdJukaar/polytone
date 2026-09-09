package net.mehvahdjukaar.polytone.content.particle;

import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

public record ParticleSpec(Optional<Holder<ParticleType<?>>> type, Optional<ParticleOptions> options) {

    private static final SchemaCodec<ParticleSpec> BY_ID = onlyWhen(SchemaCodecs.xmap(
            SchemaCodec.wrap(CodecUtils.forwardAwareHolderByNameCodec(BuiltInRegistries.PARTICLE_TYPE)),
            t -> new ParticleSpec(t, Optional.empty()), ParticleSpec::type),
            s -> s.options.isEmpty());

    private static final SchemaCodec<ParticleSpec> WITH_OPTIONS = onlyWhen(SchemaCodecs.xmap(
            SchemaCodec.wrap(ParticleTypes.CODEC),
            o -> new ParticleSpec(Optional.empty(), Optional.of(o)), s -> s.options.orElseThrow()),
            s -> s.options.isPresent());

    public static final SchemaCodec<ParticleSpec> CODEC = SchemaCodecs.alternatives(
            SchemaCodecs.alt("id", BY_ID),
            SchemaCodecs.alt("with options", WITH_OPTIONS));

    private static SchemaCodec<ParticleSpec> onlyWhen(SchemaCodec<ParticleSpec> codec, Predicate<ParticleSpec> holds) {
        return SchemaCodec.of(codec.flatComapMap(s -> s, s -> holds.test(s) ? DataResult.success(s)
                : DataResult.error(() -> "particle spec is not of this form")), codec.schema());
    }

    public boolean isEmpty() {
        return type.isEmpty() && options.isEmpty();
    }

    public ParticleType<?> particleType() {
        return options.map(ParticleOptions::getType).orElseGet(() ->(ParticleType) type.get().value());
    }

    public boolean isDynamic() {
        return type.isPresent() &&
                Polytone.CUSTOM_PARTICLES.isDynamicParticle(type.get().unwrapKey().get().location());
    }

    @SuppressWarnings("unchecked")
    public @Nullable ParticleOptions resolveOptions(@Nullable BlockState fallbackState) {
        if (options.isPresent()) return options.get();
        ParticleType<?> particleType = type.get().value();
        if (particleType instanceof SimpleParticleType simple) {
            return simple;
        }
        if (fallbackState != null) {
            if (particleType == ParticleTypes.BLOCK || particleType == ParticleTypes.FALLING_DUST
                    || particleType == ParticleTypes.BLOCK_MARKER || particleType == ParticleTypes.DUST_PILLAR) {
                return new BlockParticleOption((ParticleType<BlockParticleOption>) particleType, fallbackState);
            }
            if (particleType == ParticleTypes.ITEM) {
                return new ItemParticleOption((ParticleType<ItemParticleOption>) particleType,
                        fallbackState.getBlock().asItem().getDefaultInstance());
            }
        }
        Polytone.LOGGER.error("Particle type {} needs its own options, write it as an object instead of an id",
                particleType);
        return null;
    }
}
