package net.mehvahdjukaar.polytone.particle;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class ParticleModifiersManager extends JsonPartialReloader {

    private final Map<ParticleType<?>, ParticleModifier> particleModifiers = new HashMap<>();
    private final Map<ParticleType<?>, ParticleModifier> simpleModifiers = new HashMap<>();

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
            var res = j.getKey();
            ParticleModifier modifier = ParticleModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Particle Modifier with json res {} - error: {}",
                            res, errorMsg)).getFirst();
            var particle = Polytone.getTarget(res, BuiltInRegistries.PARTICLE_TYPE);
            if (particle != null) {
                particleModifiers.put(particle.getFirst(), modifier);
            }
        }

    }

    @Override
    protected void reset() {
        particleModifiers.clear();
        //hack
        particleModifiers.putAll(simpleModifiers);
        simpleModifiers.clear();
    }

    public void addCustomParticleColor(ResourceLocation id, String color) {
        var opt = BuiltInRegistries.PARTICLE_TYPE.getOptional(id);
        opt.ifPresent(t -> simpleModifiers.put(t, ParticleModifier.ofColor(color)));
        //hack
        particleModifiers.putAll(simpleModifiers);
    }
}
