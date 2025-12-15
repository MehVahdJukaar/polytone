package net.mehvahdjukaar.polytone.content.particle;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class OverridingParticleFactory<T extends ParticleOptions> implements ParticleProvider<T> {

    private final CustomParticleFactory customFactory;

    public OverridingParticleFactory(CustomParticleFactory factory) {
        this.customFactory = factory;
    }

    @Override
    public @Nullable Particle createParticle(T type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
        try {
            //no server override and this particle already exists so we cant use custom data here.
            var opt = new ExtraDataParticleOptions(Map.of(), type.getType());
            return customFactory.createParticle(opt, level, x, y, z, xSpeed, ySpeed, zSpeed, random);
        } catch (Exception e) {
            Polytone.LOGGER.error("Failed to create particle", e);
            return null;
        }
    }
}
