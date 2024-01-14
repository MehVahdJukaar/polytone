package net.mehvahdjukaar.polytone.colormap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.mehvahdjukaar.polytone.utils.PartialReloader;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener.scanDirectory;

public class ColormapsManager extends JsonImgPartialReloader {

    // custom defined colormaps
    private final BiMap<ResourceLocation, BlockColor> colormapsIds = HashBiMap.create();

    public ColormapsManager() {
        super("colormaps");
    }


    @Override
    public void process(Resources resources) {
        var jsons = resources.jsons();
        var textures = ArrayImage.groupTextures(resources.textures());

        Set<ResourceLocation> usedTextures = new HashSet<>();

        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            Colormap colormap = Colormap.DIRECT_CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Colormap with json id {} - error: {}",
                            id, errorMsg)).getFirst();
            fillColormapPalette(textures, id, colormap, usedTextures);
            // we need to fill these before we parse the properties as they will be referenced below
            add(id, colormap);
        }


        // creates orphaned texture colormaps
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            ResourceLocation id = t.getKey();
            Colormap defaultColormap = Colormap.createDefault(t.getValue().keySet());
            fillColormapPalette(textures, id, defaultColormap, usedTextures);
            // we need to fill these before we parse the properties as they will be referenced below
            add(id, defaultColormap);
        }
    }

    @Override
    public void reset() {
        colormapsIds.clear();
        colormapsIds.put(new ResourceLocation("grass_color"), Colormap.GRASS_COLOR);
        colormapsIds.put(new ResourceLocation("foliage_color"), Colormap.FOLIAGE_COLOR);
        colormapsIds.put(new ResourceLocation("water_color"), Colormap.WATER_COLOR);
        colormapsIds.put(new ResourceLocation("biome_sample"), Colormap.BIOME_SAMPLE);
        colormapsIds.put(new ResourceLocation("triangular_biome_sample"), Colormap.TR_BIOME_SAMPLE);
    }

    @Nullable
    public BlockColor get(ResourceLocation id) {
        return colormapsIds.get(id);
    }

    @Nullable
    public ResourceLocation getKey(BlockColor object) {
        return colormapsIds.inverse().get(object);
    }


    public void add(ResourceLocation id, Colormap colormap) {
        colormapsIds.put(id, colormap);
    }


    public static void fillColormapPalette(Map<ResourceLocation, Int2ObjectMap<ArrayImage>> textures,
                                           ResourceLocation id, Colormap colormap, Set<ResourceLocation> usedTextures) {
        var getters = colormap.getGetters();

        var textureMap = textures.get(id);

        if (textureMap != null) {
            for (var g : getters.int2ObjectEntrySet()) {
                int index = g.getIntKey();
                Colormap.Sampler tint = g.getValue();
                boolean success = false;
                if (getters.size() == 1 || index == 0) {
                    success = tryPopulatingColormap(textureMap, id, -1, tint, usedTextures);
                }
                if (!success) {
                    success = tryPopulatingColormap(textureMap, id, index, tint, usedTextures);
                }
                if (!success) {
                    throw new IllegalStateException("Could not find any colormap associated with " + id + " for tint index " + index + ". " +
                            "Expected: " + id);
                }
            }
        } else {
            throw new IllegalStateException("Could not find any colormap associated with " + id + ". " +
                    "Expected: " + id);
        }
    }

    private static boolean tryPopulatingColormap(Map<Integer, ArrayImage> textures, ResourceLocation path, int index,
                                                 Colormap.Sampler g, Set<ResourceLocation> usedTexture) {
        ArrayImage texture = textures.get(index);
        if (texture != null) {
            usedTexture.add(path);
            g.acceptTexture(texture);
            if (texture.pixels().length == 0) {
                throw new IllegalStateException("Colormap at location " + path + " had invalid 0 dimension");
            }
            return true;
        }
        return false;
    }



}
