package net.mehvahdjukaar.polytone.utils;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.companion.Naming;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
                    int pixel = nativeImage.getPixelRGBA(j, i);
                    pixelMatrix[i][j] = FastColor.ARGB32.color(
                            255,
                            FastColor.ABGR32.red(pixel),
                            FastColor.ABGR32.green(pixel),
                            FastColor.ABGR32.blue(pixel)
                    );
                }
            }
            return pixelMatrix;
        }
    }

    public static Map<ResourceLocation, Group> groupTextures(Map<ResourceLocation, ArrayImage> texturesColormap) {
        Map<ResourceLocation, Group> groupedMap = new LinkedHashMap<>();

        for (var e : texturesColormap.entrySet()) {
            ResourceLocation id = e.getKey();
            String path = id.getPath();
            // Same stem/tint-index rule as the reload driver (Naming), so grouping and filling can't
            // disagree. The old regex ran on the whole path and dropped any id with a digit before
            // the trailing "_<n>" (it fell out of grouping entirely).
            Naming.ParsedName parsed = Naming.parse(PathsUtils.lastSegment(path));
            ResourceLocation stemId = id.withPath(PathsUtils.directoryOf(path) + parsed.stem());
            groupedMap.computeIfAbsent(stemId, a -> new Group())
                    .put(parsed.index(), e.getValue());
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
