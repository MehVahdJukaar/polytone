package net.mehvahdjukaar.polytone.utils;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("all")
public record ArrayImage(int[][] pixels, int width, int height) {
    //TODO: remove this isnt needed

    public static Map<ResourceLocation, Int2ObjectMap<ArrayImage>> gatherGroupedImages(ResourceManager manager, String path) {
        return groupTextures(gatherImages(manager, path));
    }

    public static Map<ResourceLocation, ArrayImage> gatherImages(ResourceManager manager, String path) {
        Map<ResourceLocation, ArrayImage> map = new HashMap<>();

        FileToIdConverter helper = new FileToIdConverter(path, ".png");

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

        return map;
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
                            255,
                            FastColor.ARGB32.blue(pixel),
                            FastColor.ARGB32.green(pixel),
                            FastColor.ARGB32.red(pixel)
                    );
                }
            }
            return pixelMatrix;
        }
    }

    public static Map<ResourceLocation, Int2ObjectMap<ArrayImage>> groupTextures(Map<ResourceLocation, ArrayImage> texturesColormap) {
        Map<ResourceLocation, Int2ObjectMap<ArrayImage>> groupedMap = new LinkedHashMap<>();

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
                groupedMap.computeIfAbsent(new ResourceLocation(id.getNamespace(), key), a -> new Int2ObjectArrayMap<>())
                        .put(index, e.getValue());
            }
        }
        return groupedMap;
    }

}
