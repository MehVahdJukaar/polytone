package net.mehvahdjukaar.polytone.content.particle.custom;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.Level;

public interface IParticleTickable {

    <T extends Particle> void tick(T particle, Level level);
}
