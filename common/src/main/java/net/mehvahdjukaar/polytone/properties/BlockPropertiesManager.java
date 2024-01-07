package net.mehvahdjukaar.polytone.properties;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener.scanDirectory;

// Ugly ass god class
public class BlockPropertiesManager extends SimplePreparableReloadListener<BlockPropertiesManager.Resources> {

    private static final BiMap<ResourceLocation, Colormap> COLORMAPS_IDS = HashBiMap.create();

    private static final Codec<Colormap> COLORMAP_REFERENCE_CODEC = ResourceLocation.CODEC.flatXmap(
            id -> Optional.ofNullable(COLORMAPS_IDS.get(id)).map(DataResult::success)
                    .orElse(DataResult.error(() -> "Unknown Color Property with id " + id)),
            object -> Optional.ofNullable(COLORMAPS_IDS.inverse().get(object)).map(DataResult::success)
                    .orElse(DataResult.error(() -> "Unknown Color Property: " + object)));

    public static final Codec<Colormap> COLORMAP_CODEC =
            ExtraCodecs.validate(new ReferenceOrDirectCodec<>(COLORMAP_REFERENCE_CODEC, Colormap.DIRECT_CODEC,
                            i -> i.isReference = true),
                    j -> j.getters.size() == 0 ? DataResult.error(() -> "Must have at least 1 tint getter") :
                            DataResult.success(j));


    private final Gson gson = new Gson();
    private final Map<Block, BlockPropertyModifier> vanillaProperties = new HashMap<>();
    private final String root = Polytone.MOD_ID;
    private final String colormapsPath = "colormaps";
    private final String propertiesPath = "properties";

    protected record Resources(Map<ResourceLocation, JsonElement> modifiers,
                               Map<ResourceLocation, JsonElement> colormaps,
                               Map<String, ArrayImage> textures) {
    }


    /**
     * Performs any reloading that can be done off-thread, such as file IO
     * Just grabs resources like all others do. Throwing exceptions is better handled in apply()
     */
    @Override
    protected Resources prepare(ResourceManager resourceManager, ProfilerFiller profiler) {

        Map<ResourceLocation, JsonElement> colormaps = new HashMap<>();
        scanDirectory(resourceManager, root + "/" + colormapsPath, this.gson, colormaps);

        Map<ResourceLocation, JsonElement> properties = new HashMap<>();
        scanDirectory(resourceManager, root + "/" + propertiesPath, this.gson, properties);

        Map<String, ArrayImage> images = new HashMap<>();
        gatherImages(resourceManager, root, images);

        return new Resources(properties, colormaps, images);
    }

    private static void gatherImages(ResourceManager manager, String string, Map<String, ArrayImage> map) {
        FileToIdConverter helper = new FileToIdConverter(string, ".png");

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

        Map<ResourceLocation, BlockPropertyModifier> propertiesMap = new HashMap<>();

        Map<ResourceLocation, JsonElement> colormapJsons = resources.colormaps;
        Map<ResourceLocation, JsonElement> propertiesJsons = resources.modifiers;
        Map<String, ArrayImage> textures = resources.textures;


        COLORMAPS_IDS.clear();

        for (var j : colormapJsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            Colormap colormap = Colormap.DIRECT_CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Client Block Property with json id {} - error: {}",
                            id, errorMsg)).getFirst();
            fillColormapPalette(textures, colormapsPath, id, colormap);
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
                fillColormapPalette(textures, propertiesPath, id, c);
            }

            propertiesMap.put(id, prop);
        }


        resetProperties();

        applyAllModifiers(propertiesMap);

    }

    private void applyAllModifiers(Map<ResourceLocation, BlockPropertyModifier> propertiesMap) {
        for (var p : propertiesMap.entrySet()) {
            ResourceLocation id = new ResourceLocation(p.getKey().getPath().replaceFirst("/", ":"));
            var block = BuiltInRegistries.BLOCK.getOptional(id);
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
