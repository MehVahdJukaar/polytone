package net.mehvahdjukaar.polytone.particle;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;

public interface CustomParticleFactory extends ParticleProvider<ParticleOptions> {

    void setSpriteSet(ParticleEngine.MutableSpriteSet spriteSet);

    boolean forceSpawns();
}
