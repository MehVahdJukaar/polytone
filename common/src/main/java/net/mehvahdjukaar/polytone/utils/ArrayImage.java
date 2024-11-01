package net.mehvahdjukaar.polytone.utils;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("all")
public record ArrayImage(int[][] pixels, int width, int height) {
    public ArrayImage(int[][] matrix) {
        this(matrix, matrix[0].length, matrix.length);
    }
    //TODO: remove this isnt needed

    public static  Map<ResourceLocation, ArrayImage> scanDirectory(ResourceManager manager, String path) {
        Map<ResourceLocation, ArrayImage> map = new HashMap<>();
        scanDirectory(manager, path, map);
        return map;
    }

    public static void scanDirectory(ResourceManager manager, String path, Map<ResourceLocation, ArrayImage> map) {

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
                    int pixel = nativeImage.getPixel(j, i);
                    pixelMatrix[i][j] = ARGB.color(
                            255,
                            ARGB.red(pixel),
                            ARGB.green(pixel),
                            ARGB.blue(pixel)
                    );
                }
            }
            return pixelMatrix;
        }
    }

    public static Map<ResourceLocation, Group> groupTextures(Map<ResourceLocation, ArrayImage> texturesColormap) {
        Map<ResourceLocation, Group> groupedMap = new LinkedHashMap<>();

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
                groupedMap.computeIfAbsent(id.withPath(key), a -> new Group())
                        .put(index, e.getValue());
            }else{
                //no match.
                Group group = new Group();
                group.put(-1, e.getValue());
                groupedMap.put(id, group);
            }
        }
        return groupedMap;
    }

    public static class Group extends Int2ObjectArrayMap<ArrayImage> {

        public Group() {
            super();
        }

        public ArrayImage getDefault(){
            return this.get(-1);
        }
    }

}
