package net.mehvahdjukaar.polytone.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.Level;

public interface ParticleTickable {

    void tick(Particle particle, Level level);
}
