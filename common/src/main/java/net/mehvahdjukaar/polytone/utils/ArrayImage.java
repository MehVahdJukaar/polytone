package net.mehvahdjukaar.polytone.utils;

import com.mojang.blaze3d.platform.NativeImage;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@SuppressWarnings("all")
public record ArrayImage(int[][] pixels, int width, int height) {
    //TODO: remove this isnt needed

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

}
