package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface ParticleFactory {

    void createParticle(ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                        @Nullable BlockState state);


    Codec<ParticleFactory> FROM_TYPE_CODEC = BuiltInRegistries.PARTICLE_TYPE.byNameCodec().flatXmap(r -> {
        var p = ParticleFactory.fromType(r);
        return p == null ? DataResult.error(() -> "Unsupported Particle Type " + r) : DataResult.success(p);
    }, t -> DataResult.error(() -> "Encode not supported"));

    Codec<ParticleFactory> CODEC = new ReferenceOrDirectCodec<>(CustomParticlesManager.REFERENCE_CODEC, FROM_TYPE_CODEC, true);

    @Nullable
    static ParticleFactory fromType(ParticleType<?> type) {
        if (type instanceof SimpleParticleType st) {
            return (world, x, y, z, xSpeed, ySpeed, zSpeed, state) ->
                    world.addParticle(st, x, y, z, xSpeed, ySpeed, zSpeed);
        } else if (type.getDeserializer() == BlockParticleOption.DESERIALIZER) {
            return (world, x, y, z, xSpeed, ySpeed, zSpeed, state) -> {
                var o = new BlockParticleOption((ParticleType<BlockParticleOption>) type, state);
                world.addParticle(o, x, y, z, xSpeed, ySpeed, zSpeed);
            };
        } else if (type.getDeserializer() == ItemParticleOption.DESERIALIZER) {
            return (world, x, y, z, xSpeed, ySpeed, zSpeed, state) -> {
                var o = new ItemParticleOption((ParticleType<ItemParticleOption>) type, state.getBlock().asItem().getDefaultInstance());
                world.addParticle(o, x, y, z, xSpeed, ySpeed, zSpeed);
            };
        } else {
            return null;
        }
    }

}
