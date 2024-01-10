package net.mehvahdjukaar.polytone;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.biome.BiomeEffectsManager;
import net.mehvahdjukaar.polytone.block.BlockPropertiesManager;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.particle.ParticleModifiersManager;
import net.mehvahdjukaar.polytone.sound.SoundTypesManager;
import net.mehvahdjukaar.polytone.texture.VariantTextureManager;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;

import static net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener.scanDirectory;

// Ugly ass god class
public class PropertiesReloadListener extends SimplePreparableReloadListener<PropertiesReloadListener.Resources> {

    private static final String ROOT = Polytone.MOD_ID;
    private static final String COLORMAPS_PATH = "colormaps";
    private static final String SOUND_TYPE_PATH = "sound_types";
    private static final String BLOCK_PROPERTIES_PATH = "block_properties";
    private static final String BIOME_EFFECTS_PATH = "biome_effects";
    private static final String LIQUID_PATH = "liquid_properties";
    private static final String PARTICLE_PATH = "particle_modifiers";
    private static final String VARIANT_TEXTURES_PATH = "variant_textures";

    private final Gson gson = new Gson();


    protected record Resources(Map<ResourceLocation, JsonElement> blockModifiers,
                               Map<ResourceLocation, JsonElement> colormaps,
                               Map<ResourceLocation, JsonElement> soundTypes,
                               Map<ResourceLocation, JsonElement> biomeEffects,
                               Map<ResourceLocation, JsonElement> liquids,
                               Map<ResourceLocation, JsonElement> particles,
                               Map<ResourceLocation, JsonElement> variantTextures,
                               Map<ResourceLocation, List<String>> soundEvents,
                               Map<ResourceLocation, ArrayImage> textures) {
    }


    /**
     * Performs any reloading that can be done off-thread, such as file IO
     * Just grabs resources like all others do. Throwing exceptions is better handled in apply()
     */
    @Override
    protected Resources prepare(ResourceManager resourceManager, ProfilerFiller profiler) {

        Map<ResourceLocation, JsonElement> blockProperties = new HashMap<>();
        scanDirectory(resourceManager, ROOT + "/" + BLOCK_PROPERTIES_PATH, this.gson, blockProperties);

        Map<ResourceLocation, JsonElement> colormaps = new HashMap<>();
        scanDirectory(resourceManager, ROOT + "/" + COLORMAPS_PATH, this.gson, colormaps);

        Map<ResourceLocation, JsonElement> soundTypes = new HashMap<>();
        scanDirectory(resourceManager, ROOT + "/" + SOUND_TYPE_PATH, this.gson, soundTypes);

        Map<ResourceLocation, JsonElement> biomeEffects = new HashMap<>();
        scanDirectory(resourceManager, ROOT + "/" + BIOME_EFFECTS_PATH, this.gson, biomeEffects);

        Map<ResourceLocation, JsonElement> liquids = new HashMap<>();
        scanDirectory(resourceManager, ROOT + "/" + LIQUID_PATH, this.gson, liquids);

        Map<ResourceLocation, JsonElement> particles = new HashMap<>();
        scanDirectory(resourceManager, ROOT + "/" + PARTICLE_PATH, this.gson, particles);

        Map<ResourceLocation, JsonElement> variantTextures = new HashMap<>();
        scanDirectory(resourceManager, ROOT + "/" + VARIANT_TEXTURES_PATH, this.gson, variantTextures);

        var soundEvents = SoundTypesManager.gatherSoundEvents(resourceManager, ROOT);

        Map<ResourceLocation, ArrayImage> images = new HashMap<>();
        ColormapsManager.gatherImages(resourceManager, ROOT, images);

        return new Resources(blockProperties, colormaps, soundTypes, biomeEffects, liquids, particles,variantTextures, soundEvents, images);
    }


    @Override
    protected void apply(Resources resources, ResourceManager resourceManager, ProfilerFiller profiler) {

        // reset all
        BlockPropertiesManager.reset();
        BiomeEffectsManager.reset();


        Map<ResourceLocation, JsonElement> colormapJsons = resources.colormaps;
        Map<ResourceLocation, JsonElement> soundJsons = resources.soundTypes;
        Map<ResourceLocation, JsonElement> blockPropertiesJsons = resources.blockModifiers;
        Map<ResourceLocation, JsonElement> biomesJsons = resources.biomeEffects;
        Map<ResourceLocation, JsonElement> particleJsons = resources.particles;
        Map<ResourceLocation, JsonElement> variantTextures = resources.variantTextures;
        Map<ResourceLocation, List<String>> soundEvents = resources.soundEvents;

        Map<ResourceLocation, Map<Integer, ArrayImage>> groupedTextures = ColormapsManager.groupTextures(resources.textures);

        Map<ResourceLocation, Map<Integer, ArrayImage>> texturesColormap = new HashMap<>();
        Map<ResourceLocation, Map<Integer, ArrayImage>> texturesProperties = new HashMap<>();

        for (var entry : groupedTextures.entrySet()) {
            ResourceLocation id = entry.getKey();
            String path = id.getPath();
            if (path.startsWith(COLORMAPS_PATH)) {
                texturesColormap.put(id.withPath(path.replace(COLORMAPS_PATH + "/", "")), entry.getValue());
            } else {
                texturesProperties.put(id.withPath(path.replace(BLOCK_PROPERTIES_PATH + "/", "")), entry.getValue());
            }
        }

        // Registers client only sounds if needed
        SoundTypesManager.processCustomSounds(soundEvents);

        // Create defined sound types
        SoundTypesManager.process(soundJsons);

        Set<ResourceLocation> usedTextures = new HashSet<>();
        // Creates defined colormaps
        ColormapsManager.process(colormapJsons, texturesColormap, usedTextures);

        // Creates block properties blockModifiers
        BlockPropertiesManager.process(blockPropertiesJsons, texturesProperties, usedTextures);

        // Create biomes blockModifiers
        BiomeEffectsManager.process(biomesJsons);

        // Create variant textures
        VariantTextureManager.process(variantTextures);

        // Create particle modifiers
        ParticleModifiersManager.process(particleJsons);


        // Apply block properties blockModifiers
        BlockPropertiesManager.apply();

        // Apply biome blockModifiers if we have a level
        BiomeEffectsManager.tryApply();
    }


}
