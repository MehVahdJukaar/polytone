package net.mehvahdjukaar.polytone.properties;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.properties.biome.BiomeEffectsManager;
import net.mehvahdjukaar.polytone.properties.block.BlockPropertiesManager;
import net.mehvahdjukaar.polytone.properties.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.properties.sounds.SoundTypesManager;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener.scanDirectory;

// Ugly ass god class
public class PolytonePropertiesReloadListener extends SimplePreparableReloadListener<PolytonePropertiesReloadListener.Resources> {

    private static final String ROOT = Polytone.MOD_ID;
    private static final String COLORMAPS_PATH = "colormaps";
    private static final String SOUND_TYPE_PATH = "sound_types";
    private static final String BLOCK_PROPERTIES_PATH = "block_properties";
    private static final String BIOME_EFFECTS_PATH = "biomes_effects";
    private static final String LIQUID_PATH = "liquids_properties";

    private final Gson gson = new Gson();


    protected record Resources(Map<ResourceLocation, JsonElement> modifiers,
                               Map<ResourceLocation, JsonElement> colormaps,
                               Map<ResourceLocation, JsonElement> soundTypes,
                               Map<ResourceLocation, JsonElement> biomeEffects,
                               Map<ResourceLocation, JsonElement> liquids,
                               Map<ResourceLocation, ArrayImage> textures) {
    }


    /**
     * Performs any reloading that can be done off-thread, such as file IO
     * Just grabs resources like all others do. Throwing exceptions is better handled in apply()
     */
    @Override
    protected Resources prepare(ResourceManager resourceManager, ProfilerFiller profiler) {

        Map<ResourceLocation, JsonElement> colormaps = new HashMap<>();
        scanDirectory(resourceManager, ROOT + "/" + COLORMAPS_PATH, this.gson, colormaps);

        Map<ResourceLocation, JsonElement> soundTypes = new HashMap<>();
        scanDirectory(resourceManager, ROOT + "/" + SOUND_TYPE_PATH, this.gson, soundTypes);

        Map<ResourceLocation, JsonElement> blockProperties = new HashMap<>();
        scanDirectory(resourceManager, ROOT + "/" + BLOCK_PROPERTIES_PATH, this.gson, blockProperties);

        Map<ResourceLocation, JsonElement> biomeEffects = new HashMap<>();
        scanDirectory(resourceManager, ROOT + "/" + BIOME_EFFECTS_PATH, this.gson, biomeEffects);

        Map<ResourceLocation, JsonElement> liquids = new HashMap<>();
        scanDirectory(resourceManager, ROOT + "/" + LIQUID_PATH, this.gson, biomeEffects);


        Map<ResourceLocation, ArrayImage> images = new HashMap<>();
        ColormapsManager.gatherImages(resourceManager, ROOT, images);

        return new Resources(blockProperties, colormaps, biomeEffects, liquids, soundTypes, images);
    }


    @Override
    protected void apply(Resources resources, ResourceManager resourceManager, ProfilerFiller profiler) {

        BlockPropertiesManager.reset();


        Map<ResourceLocation, JsonElement> colormapJsons = resources.colormaps;
        Map<ResourceLocation, JsonElement> soundJsons = resources.soundTypes;
        Map<ResourceLocation, JsonElement> blockPropertiesJsons = resources.modifiers;
        Map<ResourceLocation, JsonElement> biomesJsons = resources.modifiers;

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

        // Create defined sound types
        SoundTypesManager.process(soundJsons);

        Set<ResourceLocation> usedTextures = new HashSet<>();
        // Creates defined colormaps
        ColormapsManager.process(colormapJsons, texturesColormap, usedTextures);

        // Creates block properties modifiers
        BlockPropertiesManager.process(blockPropertiesJsons, texturesProperties, usedTextures);

        // Create biomes modifiers
        BiomeEffectsManager.process(biomesJsons);


        // Apply block properties modifiers
        BlockPropertiesManager.apply();

        // Apply biomes modifiers if we have a level
        BiomeEffectsManager.tryApply();
    }


}
