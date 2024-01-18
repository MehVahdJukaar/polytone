package net.mehvahdjukaar.polytone.particle;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class ParticleModifiersManager extends JsonPartialReloader {

    private final Map<ParticleType<?>, ParticleModifier> particleModifiers = new HashMap<>();

    public ParticleModifiersManager() {
        super("particle_modifiers");
    }

    public void maybeModify(ParticleType<?> type, Particle particle) {
        var mod = particleModifiers.get(type);
        if (mod != null) mod.modify(particle);
    }

    @Override
    public void process(Map<ResourceLocation, JsonElement> jsons) {

        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            ParticleModifier modifier = ParticleModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Particle Modifier with json id {} - error: {}",
                            id, errorMsg)).getFirst();
            var particle = Polytone.getTarget(id, Registry.PARTICLE_TYPE);
            if (particle != null) {
                particleModifiers.put(particle.getFirst(), modifier);
            }
        }

    }

    @Override
    protected void reset() {
        particleModifiers.clear();
    }

    public void addCustomParticleColor(ResourceLocation id, String color) {
        var opt = Registry.PARTICLE_TYPE.getOptional(id);
        opt.ifPresent(t -> particleModifiers.put(t, ParticleModifier.ofColor(color)));
    }
}
