package net.mehvahdjukaar.polytone.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface CustomParticleFactory extends ParticleProvider<SimpleParticleType> {

    void setSpriteSet(ParticleEngine.MutableSpriteSet spriteSet);

    @Nullable
    Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                            @Nullable BlockState state);

    @Nullable
    default Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        return createParticle(type, level, x, y, z, xSpeed, ySpeed, zSpeed, null);
    }


    @Nullable
    default ModelResourceLocation getCustomModel() {
        return null;
    }
}
