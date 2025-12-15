package net.mehvahdjukaar.polytone.content.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface CustomParticleFactory extends ParticleProvider<ExtraDataParticleOptions> {

    void setSpriteSet(SpriteSet spriteSet);

    @Nullable
    Particle createParticleWithState(ExtraDataParticleOptions type, ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                            @Nullable BlockState state, RandomSource random);

    @Nullable
    @Override
    default Particle createParticle(ExtraDataParticleOptions type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
        return createParticleWithState(type, level, x, y, z, xSpeed, ySpeed, zSpeed, null, random);
    }


    @Nullable
    default ResourceLocation getCustomModel() {
        return null;
    }

    boolean forceSpawns();
}
