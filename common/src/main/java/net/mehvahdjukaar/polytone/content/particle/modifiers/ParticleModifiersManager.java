package net.mehvahdjukaar.polytone.content.particle.modifiers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.companion.TexturePart;
import net.mehvahdjukaar.polytone.common.companion.TrackedTextures;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParticleModifiersManager extends ContentManager<ParticleModifier> {

    private final Multimap<ParticleType<?>, ParticleModifier> particleModifiers = HashMultimap.create();
    @Nullable
    private JsonElement xpOrbReplaceJson = null;
    @Nullable
    private ParticleOptions xpOrbReplaceParticle = null;

    private static final TexturePart<ParticleModifier> COLORMAP =
            TexturePart.plain(ParticleModifier::getColormap);

    public ParticleModifiersManager() {
        super(Spec.of("Particle modifier", () -> ParticleModifier.CODEC)
                .wikiPage("Particle-Modifiers")
                .textureParts(COLORMAP)
                .folders("particle_modifiers"));
    }

    public void maybeModify(ParticleOptions options, Level level, @NotNull Particle particle) {
        if (particle instanceof SingleQuadParticle sqp) {
            var mod = particleModifiers.get(options.getType());
            for (var modifier : mod) {
                modifier.modify(sqp, level, options);
            }
        }
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        var jsons = resources.jsons();
        var textures = new TrackedTextures(resources.textures());

        Parsed.SortedMap<ParticleModifier> parsedModifiers =
                parseJsonsOrPartial(jsons, ParticleModifier.PARTIAL_CODEC, ops);


        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers.entrySet()) {
            Identifier id = entry.getKey();
            Parsed<ParticleModifier> parsed = entry.getValue();
            ParticleModifier modifier = parsed.getResultOrPartial();

            // auto-attach a default colormap when a texture exists but none is declared,
            // then fill inline colormaps from the scanned textures
            if (!contentTexture.adoptable(textures, id, modifier).isEmpty()) {
                modifier.setColormap(Colormap.createDefTriangle());
            }
            contentTexture.fill(textures, id, modifier, true);

            if (parsed.isEnabled()) this.addModifier(id, modifier);
        }

        // creates orphaned texture colormaps & properties
        for (var orphan : contentTexture.orphans(textures, parsedModifiers.keySet())) {
            ParticleModifier modifier = ParticleModifier.ofColormap(Colormap.createDefTriangle());
            contentTexture.fill(textures, orphan.stemId(), modifier, true);
            addModifier(orphan.stemId(), modifier);
        }

        if (this.xpOrbReplaceJson != null) {

            var v = Parsed.parseAlways(ParticleTypes.CODEC, xpOrbReplaceJson,
                    ops, Identifier.withDefaultNamespace("xp_orb"), "XP orb modifier");
            if (v.isEnabled()) {
                this.xpOrbReplaceParticle = v.getResultOrPartial();
            }
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
        if (!particleModifiers.isEmpty()) {
            Polytone.LOGGER.info("Registered {} particle modifiers", particleModifiers.size());
        }
    }


    private void addModifier(Identifier pathId, ParticleModifier mod) {
        for (var particle : mod.targets().compute(pathId, BuiltInRegistries.PARTICLE_TYPE)) {
            //can have multiple
            particleModifiers.put(particle.value(), mod);
        }
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        particleModifiers.clear();
    }

    public void addCustomParticleColor(Identifier id, String color) {
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
