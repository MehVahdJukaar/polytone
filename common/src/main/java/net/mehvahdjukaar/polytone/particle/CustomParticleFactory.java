package net.mehvahdjukaar.polytone.particle;

import net.minecraft.client.particle.ParticleEngine;

public interface CustomParticleFactory extends ParticleFactory {
    void setSpriteSet(ParticleEngine.MutableSpriteSet spriteSet);
}
