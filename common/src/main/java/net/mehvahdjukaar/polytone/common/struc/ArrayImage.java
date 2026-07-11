package net.mehvahdjukaar.polytone.common.struc;

import com.mojang.blaze3d.platform.NativeImage;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("all")
public record ArrayImage(int[][] pixels, int width, int height) {
    public ArrayImage(int[][] matrix) {
        this(matrix, matrix[0].length, matrix.length);
    }

    public static  Map<Identifier, ArrayImage> scanDirectory(ResourceManager manager, String path) {
        Map<Identifier, ArrayImage> map = new HashMap<>();
        scanDirectory(manager, path, map);
        return map;
    }

    public static void scanDirectory(ResourceManager manager, String path, Map<Identifier, ArrayImage> map) {

        FileToIdConverter helper = new FileToIdConverter(path, ".png");

        for (Map.Entry<Identifier, Resource> entry : helper.listMatchingResources(manager).entrySet()) {
            Identifier fileId = entry.getKey();
            Identifier id = helper.fileToId(fileId);

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

}
