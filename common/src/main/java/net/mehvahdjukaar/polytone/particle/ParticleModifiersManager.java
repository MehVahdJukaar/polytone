package net.mehvahdjukaar.polytone.particle;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.mehvahdjukaar.polytone.utils.Parsed;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.mehvahdjukaar.polytone.utils.Utils;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ParticleModifiersManager extends JsonImgPartialReloader {

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
    protected void parseWithLevel(Resources resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        var jsons = resources.jsons();
        var textures = new HashMap<>(resources.textures());

        Set<ResourceLocation> usedTextures = new HashSet<>();
        Map<ResourceLocation, Parsed<ParticleModifier>> parsedModifiers = Utils.sortedMap();
        for (var j : jsons.entrySet()) {
            JsonElement json = j.getValue();
            ResourceLocation id = j.getKey();

            var modifier = Parsed.parseOptionalOrPartial(ParticleModifier.CODEC,
                    ParticleModifier.PARTIAL_CODEC, json, ops, id, "particle modifier");

            //always have priority
            parsedModifiers.put(id, modifier);
        }

        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers.entrySet()) {
            ResourceLocation id = entry.getKey();
            Parsed<ParticleModifier> parsed = entry.getValue();
            ParticleModifier modifier = parsed.getResultOrPartial();

            if (!modifier.hasColormap() && textures.containsKey(id)) {
                //if this map doesn't have a colormap defined, we set it to the default impl IF there's a texture it can use
                modifier.setColormap(Colormap.createDefTriangle());
            }

            //fill inline colormaps colormapTextures
            BlockColor tint = modifier.getColormap();
            ColormapsManager.tryAcceptingTexture(textures, id, tint, usedTextures, true);

            if (parsed.isEnabled()) this.addModifier(id, modifier);
        }

        // creates orphaned texture colormaps & properties
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            ResourceLocation id = t.getKey();
            Colormap defaultColormap = Colormap.createDefTriangle();
            ColormapsManager.tryAcceptingTexture(textures, id, defaultColormap, usedTextures, true);

            addModifier(id, ParticleModifier.ofColormap(defaultColormap));
        }
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
