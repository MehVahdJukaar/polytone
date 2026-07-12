package net.mehvahdjukaar.polytone.content.particle.custom;

import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;

public interface ICustomParticleFactory extends ParticleProvider<ParticleOptions> {

    void setSpriteSet(SpriteSet spriteSet);

    boolean isValid();

    boolean forceSpawns();
}
