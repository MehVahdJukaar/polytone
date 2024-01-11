package net.mehvahdjukaar.polytone.colormap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColormapsManager {


    // custom defined colormaps
    private static final BiMap<ResourceLocation, BlockColor> COLORMAPS_IDS = HashBiMap.create();

    public static void process(Map<ResourceLocation, JsonElement> colormapJsons,
                               Map<ResourceLocation, Map<Integer, ArrayImage>> texturesColormap,
                               Set<ResourceLocation> usedTextures) {
        clear();

        for (var j : colormapJsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            Colormap colormap = Colormap.DIRECT_CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Colormap with json id {} - error: {}",
                            id, errorMsg)).getFirst();
            fillColormapPalette(texturesColormap, id, colormap, usedTextures);
            // we need to fill these before we parse the properties as they will be referenced below
            add(id, colormap);
        }


        // creates orphaned texture colormaps
        texturesColormap.keySet().removeAll(usedTextures);

        for (var t : texturesColormap.entrySet()) {
            ResourceLocation id = t.getKey();
            Colormap defaultColormap = Colormap.createDefault(t.getValue().keySet());
            fillColormapPalette(texturesColormap, id, defaultColormap, usedTextures);
            // we need to fill these before we parse the properties as they will be referenced below
            add(id, defaultColormap);
        }
    }

    public static void clear() {
        COLORMAPS_IDS.clear();
        COLORMAPS_IDS.put(new ResourceLocation("grass_color"), Colormap.GRASS_COLOR);
        COLORMAPS_IDS.put(new ResourceLocation("foliage_color"), Colormap.FOLIAGE_COLOR);
        COLORMAPS_IDS.put(new ResourceLocation("water_color"), Colormap.WATER_COLOR);
        COLORMAPS_IDS.put(new ResourceLocation("biome_sample"), Colormap.BIOME_SAMPLE);
        COLORMAPS_IDS.put(new ResourceLocation("triangular_biome_sample"), Colormap.TR_BIOME_SAMPLE);
    }

    @Nullable
    public static BlockColor get(ResourceLocation id) {
        return COLORMAPS_IDS.get(id);
    }

    @Nullable
    public static ResourceLocation getKey(BlockColor object) {
        return COLORMAPS_IDS.inverse().get(object);
    }


    public static void add(ResourceLocation id, Colormap colormap) {
        COLORMAPS_IDS.put(id, colormap);
    }

    public static void gatherImages(ResourceManager manager, String string, Map<ResourceLocation, ArrayImage> map) {
        FileToIdConverter helper = new FileToIdConverter(string, ".png");

        for (Map.Entry<ResourceLocation, Resource> entry : helper.listMatchingResources(manager).entrySet()) {
            ResourceLocation fileId = entry.getKey();
            ResourceLocation id = helper.fileToId(fileId);

            try (InputStream inputStream = entry.getValue().open();
                 NativeImage nativeImage = NativeImage.read(inputStream)) {
                int[][] pixels = makePixelMatrix(nativeImage);

                ArrayImage image = new ArrayImage(pixels, nativeImage.getWidth(), nativeImage.getHeight());
                ArrayImage oldImage = map.put(id, image);
                if (oldImage != null) {
                    throw new IllegalStateException("Duplicate data file ignored with ID " + id);
                }
            } catch (IllegalArgumentException | IOException | UnsupportedOperationException var14) {
                Polytone.LOGGER.error("Couldn't parse texture file {} from {}", id, fileId, var14);
            }
        }
    }

    //basically just swaps the color format
    private static int[][] makePixelMatrix(NativeImage nativeImage) {
        if (nativeImage.format() != NativeImage.Format.RGBA) {
            throw new UnsupportedOperationException("Can only call makePixelMatrix for RGBA images.");
        } else {
            int width = nativeImage.getWidth();
            int height = nativeImage.getHeight();
            int[][] pixelMatrix = new int[height][width];

            for (int i = 0; i < height; ++i) {
                for (int j = 0; j < width; ++j) {
                    int pixel = nativeImage.getPixelRGBA(j, i);
                    pixelMatrix[i][j] = FastColor.ARGB32.color(
                            FastColor.ABGR32.alpha(pixel),
                            FastColor.ABGR32.red(pixel),
                            FastColor.ABGR32.green(pixel),
                            FastColor.ABGR32.blue(pixel)
                    );
                }
            }
            return pixelMatrix;
        }
    }


    public static Map<ResourceLocation, Map<Integer, ArrayImage>> groupTextures(Map<ResourceLocation, ArrayImage> texturesColormap) {
        Map<ResourceLocation, Map<Integer, ArrayImage>> groupedMap = new HashMap<>();

        Pattern pattern = Pattern.compile("(\\D+)(_\\d+)?");
        for (var e : texturesColormap.entrySet()) {
            ResourceLocation id = e.getKey();
            String str = id.getPath();
            Matcher matcher = pattern.matcher(str);
            if (matcher.matches()) {
                String key = matcher.group(1); // Group 1: the word before underscore (if any)
                String indexMatch = matcher.group(2); // Group 2: the underscore and digits (if any)

                int index = -1; // Default index if there's no underscore and digits
                if (indexMatch != null) {
                    // Extracting the index from the matched group (removing the underscore)
                    index = Integer.parseInt(indexMatch.substring(1));
                }

                // Creating or retrieving the Int2Object map for the key
                groupedMap.computeIfAbsent(id.withPath(key), a -> new HashMap<>()).put(index, e.getValue());
            }
        }
        return groupedMap;
    }


    public static void fillColormapPalette(Map<ResourceLocation, Map<Integer, ArrayImage>> textures,
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
