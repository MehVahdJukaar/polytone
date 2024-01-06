package net.mehvahdjukaar.polytone.properties;

import com.google.common.collect.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.resources.LegacyStuffWrapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;

import java.util.*;

import static net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener.scanDirectory;

// Ugly ass god class
public class BlockPropertiesManager extends SimplePreparableReloadListener<Map<ResourceLocation, BlockPropertyModifier>> {

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
    private final String colormapsPath = Polytone.MOD_ID + "/colormaps";
    private final String propertiesPath = Polytone.MOD_ID + "/properties";


    /**
     * Performs any reloading that can be done off-thread, such as file IO
     */
    @Override
    protected Map<ResourceLocation, BlockPropertyModifier> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, BlockPropertyModifier> parsedObjects = new HashMap<>();

        Map<ResourceLocation, JsonElement> colormaps = new HashMap<>();
        scanDirectory(resourceManager, colormapsPath, this.gson, colormaps);

        COLORMAPS_IDS.clear();

        for (var j : colormaps.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            Colormap colormap = Colormap.DIRECT_CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Client Block Property with json id {} - error: {}",
                            id, errorMsg)).getFirst();
            fillColormapPalette(resourceManager, colormapsPath, id, colormap);
            // we need to fill these before we parse the properties as they will be referenced below
            COLORMAPS_IDS.put(id, colormap);
        }


        // we need to decode directly as we need to check which of the don't have a texture
        Map<ResourceLocation, JsonElement> properties = new HashMap<>();
        scanDirectory(resourceManager, propertiesPath, this.gson, properties);

        for (var j : properties.entrySet()) {
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
                fillColormapPalette(resourceManager, propertiesPath, id, c);
            }

            parsedObjects.put(id, prop);
        }

        return parsedObjects;
    }

    private void fillColormapPalette(ResourceManager resourceManager, String folder, ResourceLocation id, Colormap colormap) {
        var getters = colormap.getGetters();

        for (var g : getters.int2ObjectEntrySet()) {
            boolean success = false;
            int index = g.getIntKey();
            Colormap.ColormapTintGetter tint = g.getValue();
            if (getters.size() == 1 || index == 0) {
                String path = folder + "/" + id.getPath() + ".png";
                success = tryPopulatingColormap(resourceManager, path, tint);
            }
            String path = folder + "/" + id.getPath() + "_" + index + ".png";
            if (!success) {
                success = tryPopulatingColormap(resourceManager, path, tint);
            }
            if (!success) {
                throw new IllegalStateException("Could not find any colormap associated with " + id + " for tint index " + index + ". " +
                        "Expected: " + path);
            }
        }
    }

    private static boolean tryPopulatingColormap(ResourceManager resourceManager, String path, Colormap.ColormapTintGetter g) {
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(path, (r) -> true);
        if (!resources.isEmpty()) {
            try {
                var first = resources.entrySet().stream().findFirst().get();
                g.pixels = LegacyStuffWrapper.getPixels(resourceManager, first.getKey());
                if (g.pixels.length != 256 * 256) {
                    throw new IllegalStateException("Colormap at location " + path + " had invalid dimensions. Expected 64x64");
                }
                return true;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to parse colormap at " + path);
            }
        }
        return false;
    }


    @Override
    protected void apply(Map<ResourceLocation, BlockPropertyModifier> propertiesMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        resetProperties();

     if(true)   throw new RuntimeException("aa");
        Multimap<BlockColor, Block> colorsForBlocks = LinkedHashMultimap.create();
        Set<Block> blocksToOverrideColors = new HashSet<>();
        for (var p : propertiesMap.entrySet()) {
            ResourceLocation id = new ResourceLocation(p.getKey().getPath().replaceFirst("/", ":"));
            var block = BuiltInRegistries.BLOCK.getOptional(id);
            if (block.isPresent()) {
                Block b = block.get();
                BlockPropertyModifier value = p.getValue();
                vanillaProperties.put(b, value.apply(b));


                var color = value.tintGetter();
                if(color.isPresent()) {
                    colorsForBlocks.put(color.get(), b);
                    blocksToOverrideColors.add(b);
                }
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
