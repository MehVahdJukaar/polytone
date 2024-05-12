package net.mehvahdjukaar.polytone.particle;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ParticleModifiersManager extends JsonPartialReloader {

    private final Multimap<ParticleType<?>, ParticleModifier> particleModifiers = HashMultimap.create();

    public ParticleModifiersManager() {
        super("particle_modifiers");
    }

    public void maybeModify(ParticleOptions options, Level level, Particle particle) {
        var mod = particleModifiers.get(options.getType());
        for (var modifier : mod) {
            modifier.modify(particle, level, options);
        }
    }

    @Override
    public void process(Map<ResourceLocation, JsonElement> jsons) {

        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            ParticleModifier modifier = ParticleModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Particle Modifier with json id " + id + "\n error: " + errorMsg))
                    .getFirst();
            addModifier(id, modifier);
        }
    }


    private void addModifier(ResourceLocation pathId, ParticleModifier mod) {
        var explTargets = mod.explicitTargets;
        var pathTarget = BuiltInRegistries.PARTICLE_TYPE.getOptional(pathId);
        if (explTargets.isPresent()) {
            if (pathTarget.isPresent()) {
                Polytone.LOGGER.error("Found Particle Modifier with Explicit Targets ({}) also having a valid IMPLICIT Path Target ({})." +
                        "Consider moving it under your OWN namespace to avoid overriding other packs modifiers with the same path", explTargets.get(), pathId);
            }
            for (var explicitId : explTargets.get()) {
                var target = BuiltInRegistries.PARTICLE_TYPE.getOptional(explicitId);
                target.ifPresent(type -> particleModifiers.put(type, mod));
            }
        }
        //no explicit targets. use its own ID instead
        else {
            pathTarget.ifPresent(type -> particleModifiers.put(type, mod));
        }
    }

    @Override
    protected void reset() {
        particleModifiers.clear();
    }

    public void addCustomParticleColor(ResourceLocation id, String color) {
        var opt = BuiltInRegistries.PARTICLE_TYPE.getOptional(id);
        opt.ifPresent(t -> particleModifiers.put(t, ParticleModifier.ofColor(color)));
    }
}
