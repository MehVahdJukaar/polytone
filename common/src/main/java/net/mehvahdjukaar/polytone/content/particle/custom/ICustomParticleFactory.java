package net.mehvahdjukaar.polytone.content.particle.custom;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface ICustomParticleFactory extends ParticleProvider<ExtraDataParticleOptions> {

    void setSpriteSet(SpriteSet spriteSet);

    @Nullable
    Particle createParticleWithState(ExtraDataParticleOptions type, ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                            @Nullable BlockState state, RandomSource random);

    @Nullable
    @Override
    default Particle createParticle(ExtraDataParticleOptions type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
        return createParticleWithState(type, level, x, y, z, xSpeed, ySpeed, zSpeed, null, random);
    }

    boolean isValid();

    @Nullable
    default Identifier getCustomModel() {
        return null;
    }

    boolean forceSpawns();
}
