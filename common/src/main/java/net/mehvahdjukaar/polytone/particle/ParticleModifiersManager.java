package net.mehvahdjukaar.polytone.particle;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Map;

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
    public void process(Map<ResourceLocation, JsonElement> jsons, DynamicOps<JsonElement> ops) {

        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            ParticleModifier modifier = ParticleModifier.CODEC.decode(ops, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Particle Modifier with json id {} - error: {}",
                            id, errorMsg)).getFirst();
            addModifier(id, modifier);
        }

        //TODO: add inline colormap support
        //does not support inline colormaps yet
    }


    private void addModifier(ResourceLocation pathId, ParticleModifier mod) {
        for (var p : mod.getTargets(pathId, BuiltInRegistries.PARTICLE_TYPE)) {
            //can have multiple
            particleModifiers.put(p, mod);
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
