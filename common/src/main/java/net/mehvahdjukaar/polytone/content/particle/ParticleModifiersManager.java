package net.mehvahdjukaar.polytone.content.particle;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.ColormapTextures;
import net.mehvahdjukaar.polytone.companion.TrackedTextures;
import net.mehvahdjukaar.polytone.utils.AssetsFiles;
import net.mehvahdjukaar.polytone.utils.ContentManager;
import net.mehvahdjukaar.polytone.utils.Parsed;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParticleModifiersManager extends ContentManager<ParticleModifier, AssetsFiles> {

    private final Multimap<ParticleType<?>, ParticleModifier> particleModifiers = HashMultimap.create();
    @Nullable
    private JsonElement xpOrbReplaceJson = null;
    @Nullable
    private ParticleOptions xpOrbReplaceParticle = null;

    public ParticleModifiersManager() {
        super(Spec.of("Particle modifier", () -> ParticleModifier.CODEC)
                .wikiPage("Particle-Modifiers")
                .companions(ColormapTextures.singleTexture(
                        (ParticleModifier m) -> m.getColormap(), "", "default"))
                .folders("particle_modifiers"));
    }

    @Override
    protected AssetsFiles prepare(ResourceManager resourceManager) {
        return new AssetsFiles(this.getJsonsInDirectories(resourceManager),
                this.getImagesInDirectories(resourceManager));
    }

    public void maybeModify(ParticleOptions options, Level level, @NotNull Particle particle) {
        var mod = particleModifiers.get(options.getType());
        for (var modifier : mod) {
            modifier.modify(particle, level, options);
        }
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
        var jsons = resources.jsons();
        var textures = new TrackedTextures(resources.textures());

        Parsed.SortedMap<ParticleModifier> parsedModifiers =
                Parsed.batchParseOrPartial(jsons, ParticleModifier.CODEC,
                        ParticleModifier.PARTIAL_CODEC, ops, "particle modifier");


        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers.entrySet()) {
            ResourceLocation id = entry.getKey();
            Parsed<ParticleModifier> parsed = entry.getValue();
            ParticleModifier modifier = parsed.getResultOrPartial();

            if (!modifier.hasColormap()
                    && ColormapTextures.hasUsableTexture(companions, textures, id)) {
                //if this map doesn't have a colormap defined, we set it to the default impl IF there's a texture it can use
                modifier.setColormap(Colormap.createDefTriangle());
            }

            //fill inline colormaps colormapTextures
            ColormapTextures.fill(companions, textures, id, modifier, true);

            if (parsed.isEnabled()) this.addModifier(id, modifier);
        }

        // creates orphaned texture colormaps & properties
        for (var t : textures.unused().entrySet()) {
            ResourceLocation id = t.getKey();
            Colormap defaultColormap = Colormap.createDefTriangle();
            ColormapTextures.fillDirect(textures, id, t.getValue(), defaultColormap);

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
