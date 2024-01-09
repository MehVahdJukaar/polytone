package net.mehvahdjukaar.polytone.properties;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.NewStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static net.mehvahdjukaar.polytone.NewStuff.scanDirectory;

// Ugly ass god class
public class BlockPropertiesManager extends SimplePreparableReloadListener<BlockPropertiesManager.Resources> {

    // custom defined colormaps
    public static final BiMap<ResourceLocation, BlockColor> COLORMAPS_IDS = HashBiMap.create();
    // custom defined sound types
    public static final BiMap<ResourceLocation, SoundType> SOUND_TYPES_IDS = HashBiMap.create();


    private static final String ROOT = Polytone.MOD_ID;
    private static final String COLORMAPS_PATH = "colormaps";
    private static final String SOUND_TYPE_PATH = "sound_types";
    private static final String PROPERTIES_PATH = "block_properties";

    private final Gson gson = new Gson();
    private final Map<Block, BlockPropertyModifier> vanillaProperties = new HashMap<>();


    protected record Resources(Map<ResourceLocation, JsonElement> modifiers,
                               Map<ResourceLocation, JsonElement> colormaps,
                               Map<ResourceLocation, JsonElement> soundTypes,
                               Map<String, ArrayImage> textures) {
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

        Map<ResourceLocation, JsonElement> properties = new HashMap<>();
        scanDirectory(resourceManager, ROOT + "/" + PROPERTIES_PATH, this.gson, properties);

        Map<String, ArrayImage> images = new HashMap<>();
        gatherImages(resourceManager, ROOT, images);

        return new Resources(properties, colormaps, soundTypes, images);
    }

    private static void gatherImages(ResourceManager manager, String string, Map<String, ArrayImage> map) {
        NewStuff.FileToIdConverter helper = new NewStuff. FileToIdConverter(string, ".png");

        for (Map.Entry<ResourceLocation, Resource> entry : helper.listMatchingResources(manager).entrySet()) {
            ResourceLocation fileId = entry.getKey();
            ResourceLocation id = helper.fileToId(fileId);

            try (InputStream inputStream = entry.getValue().open();
                 NativeImage nativeImage = NativeImage.read(inputStream)) {
                int[] pixels = nativeImage.makePixelArray();

                ArrayImage image = new ArrayImage(pixels, nativeImage.getWidth(), nativeImage.getHeight());
                ArrayImage oldImage = map.put(id.getPath(), image);
                if (oldImage != null) {
                    throw new IllegalStateException("Duplicate data file ignored with ID " + id);
                }
            } catch (IllegalArgumentException | IOException | UnsupportedOperationException var14) {
                Polytone.LOGGER.error("Couldn't parse texture file {} from {}", id, fileId, var14);
            }
        }
    }

    private void fillColormapPalette(Map<String, ArrayImage> textures, String folder, ResourceLocation id, Colormap colormap) {
        var getters = colormap.getGetters();

        for (var g : getters.int2ObjectEntrySet()) {
            boolean success = false;
            int index = g.getIntKey();
            Colormap.ColormapTintGetter tint = g.getValue();
            if (getters.size() == 1 || index == 0) {
                String path = folder + "/" + id.getPath();
                success = tryPopulatingColormap(textures, path, tint);
            }
            String path = folder + "/" + id.getPath() + "_" + index;
            if (!success) {
                success = tryPopulatingColormap(textures, path, tint);
            }
            if (!success) {
                throw new IllegalStateException("Could not find any colormap associated with " + id + " for tint index " + index + ". " +
                        "Expected: " + path);
            }
        }
    }

    private static boolean tryPopulatingColormap(Map<String, ArrayImage> textures, String path, Colormap.ColormapTintGetter g) {
        ArrayImage texture = textures.get(path);
        if (texture != null) {
            g.image = texture;
            if (texture.pixels().length == 0) {
                throw new IllegalStateException("Colormap at location " + path + " had invalid 0 dimension");
            }
            return true;
        }
        return false;
    }


    @Override
    protected void apply(Resources resources, ResourceManager resourceManager, ProfilerFiller profiler) {

        resetProperties();

        Map<ResourceLocation, BlockPropertyModifier> propertiesMap = new HashMap<>();

        Map<ResourceLocation, JsonElement> colormapJsons = resources.colormaps;
        Map<ResourceLocation, JsonElement> soundJsons = resources.soundTypes;
        Map<ResourceLocation, JsonElement> propertiesJsons = resources.modifiers;
        Map<String, ArrayImage> textures = resources.textures;

        SOUND_TYPES_IDS.clear();

        for (var j : soundJsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            SoundType soundType = SoundTypeHelper.DIRECT_CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Sound Type with json id {} - error: {}",
                            id, errorMsg)).getFirst();
            SOUND_TYPES_IDS.put(id, soundType);
        }

        COLORMAPS_IDS.clear();
        COLORMAPS_IDS.put(new ResourceLocation("grass_color"), Colormap.GRASS_COLOR);
        COLORMAPS_IDS.put(new ResourceLocation("foliage_color"), Colormap.FOLIAGE_COLOR);
        COLORMAPS_IDS.put(new ResourceLocation("water_color"), Colormap.WATER_COLOR);
        COLORMAPS_IDS.put(new ResourceLocation("biome_sample"), Colormap.BIOME_SAMPLE);

        for (var j : colormapJsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            Colormap colormap = Colormap.DIRECT_CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Colormap with json id {} - error: {}",
                            id, errorMsg)).getFirst();
            fillColormapPalette(textures, COLORMAPS_PATH, id, colormap);
            // we need to fill these before we parse the properties as they will be referenced below
            COLORMAPS_IDS.put(id, colormap);
        }

        for (var j : propertiesJsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            BlockPropertyModifier prop = BlockPropertyModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    // log error if parse fails
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Client Block Property with json id {} - error: {}", id, errorMsg))
                    // add loot modifier if parse succeeds
                    .getFirst();

            //fill inline colormaps textures
            var colormap = prop.tintGetter();
            if (colormap.isPresent() && colormap.get() instanceof Colormap c && !c.isReference) {
                fillColormapPalette(textures, PROPERTIES_PATH, id, c);
            }

            propertiesMap.put(id, prop);
        }


        applyAllModifiers(propertiesMap);
    }

    private void applyAllModifiers(Map<ResourceLocation, BlockPropertyModifier> propertiesMap) {
        for (var p : propertiesMap.entrySet()) {
            ResourceLocation id = new ResourceLocation(p.getKey().getPath().replaceFirst("/", ":"));
            var block = Registry.BLOCK.getOptional(id);
            if (block.isPresent()) {
                Block b = block.get();
                BlockPropertyModifier value = p.getValue();
                vanillaProperties.put(b, value.apply(b));
            }
        }
    }

    private void resetProperties() {
        for (var e : vanillaProperties.entrySet()) {
            e.getValue().apply(e.getKey());
        }
        vanillaProperties.clear();
    }
}
