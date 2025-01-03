package net.mehvahdjukaar.polytone.particle;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.Parsed;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ParticleModifiersManager extends JsonPartialReloader {

    private final Multimap<ParticleType<?>, ParticleModifier> particleModifiers = HashMultimap.create();

    public ParticleModifiersManager() {
        super("particle_modifiers");
    }

    public void maybeModify(ParticleOptions options, Level level, @NotNull Particle particle) {
        var mod = particleModifiers.get(options.getType());
        for (var modifier : mod) {
            modifier.modify(particle, level, options);
        }
    }

    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            var modifier = Parsed.parseOptional(ParticleModifier.CODEC, json, ops, id, "particle modifier");
            modifier.ifPresent(m -> this.addModifier(id, m));
        }

        //TODO: add inline colormap support
        //does not support inline colormaps yet
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
        if (!particleModifiers.isEmpty()) {
            Polytone.LOGGER.info("Registered {} particle modifiers", particleModifiers.size());
        }
    }


    private void addModifier(ResourceLocation pathId, ParticleModifier mod) {
        for (var particle : mod.targets().compute(pathId, BuiltInRegistries.PARTICLE_TYPE)) {
            //can have multiple
            particleModifiers.put(particle.value(), mod);
        }
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        particleModifiers.clear();
    }

    public void addCustomParticleColor(ResourceLocation id, String color) {
        var opt = BuiltInRegistries.PARTICLE_TYPE.getOptional(id);
        opt.ifPresent(t -> particleModifiers.put(t, ParticleModifier.ofColor(color)));
    }
}
