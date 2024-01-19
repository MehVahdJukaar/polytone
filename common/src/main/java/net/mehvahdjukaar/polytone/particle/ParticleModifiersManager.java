package net.mehvahdjukaar.polytone.particle;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
                if(target.isPresent()) {
                    var old = particleModifiers.put(target.get(), mod);
                    if(old != null){
                        Polytone.LOGGER.info("Found 2 Particle Modifiers with same target ({}). Overriding", explicitId);
                    }
                }
            }
        }
        //no explicit targets. use its own ID instead
        else {
            if(pathTarget.isPresent()) {
                var old = particleModifiers.put(pathTarget.get(), mod);
                if(old != null){
                    Polytone.LOGGER.info("Found 2 Particle Modifiers with same target ({}). Overriding", pathTarget);
                }
            }
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
