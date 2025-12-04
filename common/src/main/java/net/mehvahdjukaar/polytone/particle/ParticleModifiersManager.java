package net.mehvahdjukaar.polytone.particle;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.mehvahdjukaar.polytone.utils.Parsed;
import net.mehvahdjukaar.polytone.utils.Utils;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.gui.screens.social.PlayerEntry;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TotemParticle;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ParticleModifiersManager extends JsonImgPartialReloader {

    private final Multimap<ParticleType<?>, ParticleModifier> particleModifiers = HashMultimap.create();
    @Nullable
    private JsonElement xpOrbReplaceJson = null;
    @Nullable
    private ParticleOptions xpOrbReplaceParticle = null;

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
    protected void parseWithLevel(Resources resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
        var jsons = resources.jsons();
        var textures = new HashMap<>(resources.textures());

        Set<ResourceLocation> usedTextures = new HashSet<>();

        Parsed.SortedMap<ParticleModifier> parsedModifiers =
                Parsed.batchParseOrPartial(jsons, ParticleModifier.CODEC,
                        ParticleModifier.PARTIAL_CODEC, ops, "particle modifier");


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

        if (this.xpOrbReplaceJson != null) {

            var v = Parsed.parseAlways(ParticleTypes.CODEC, xpOrbReplaceJson,
                    ops, ResourceLocation.withDefaultNamespace("xp_orb"), "XP orb modifier");
            if (v.isEnabled()) {
                this.xpOrbReplaceParticle = v.getResultOrPartial();
            }
        }
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
        if (!particleModifiers.isEmpty()) {
            Polytone.LOGGER.info("Registered {} particle modifiers", particleModifiers.size());
        }
    }


    private void addModifier(ResourceLocation pathId, ParticleModifier mod) {
        for (var p : mod.targets().compute(pathId, BuiltInRegistries.PARTICLE_TYPE.asLookup())) {
            //can have multiple
            particleModifiers.put(p.value(), mod);
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

    public void setXpOrbReplace(JsonElement json) {
        this.xpOrbReplaceJson = json;
    }

    //TODO: finish or merge with entity modifiers particle emitters
    @Nullable
    public ParticleOptions getXpOrbReplaceParticle() {
        return xpOrbReplaceParticle;
    }
}
