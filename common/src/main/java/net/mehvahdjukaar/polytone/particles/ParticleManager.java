package net.mehvahdjukaar.polytone.particles;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class ParticleManager {

    private static final Map<ParticleType<?>, ParticleModifier> PARTICLE_MODIFIERS = new HashMap<>();

    public static void modify(ParticleType<?> type, Particle particle) {
        var mod = PARTICLE_MODIFIERS.get(type);
        if (mod != null) mod.modify(particle);
    }

    public static void process(Map<ResourceLocation, JsonElement> particleJsons) {

        PARTICLE_MODIFIERS.clear();

        for (var j : particleJsons.entrySet()) {
            var json = j.getValue();
            var res = j.getKey();
            ParticleModifier modifier = ParticleModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Particle Modifier with json res {} - error: {}",
                            res, errorMsg)).getFirst();
            var particle = Polytone.getTarget(res, BuiltInRegistries.PARTICLE_TYPE);
            if (particle != null) {
                PARTICLE_MODIFIERS.put(particle.getFirst(), modifier);
            }
        }

    }

    public static void addCustomParticleColor(ResourceLocation id, String color) {
        var opt = BuiltInRegistries.PARTICLE_TYPE.getOptional(id);
        opt.ifPresent(t -> PARTICLE_MODIFIERS.put(t, ParticleModifier.ofColor(color)));
    }
}
