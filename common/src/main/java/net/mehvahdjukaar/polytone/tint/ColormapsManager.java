package net.mehvahdjukaar.polytone.tint;

import net.minecraft.client.resources.LegacyStuffWrapper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ColormapsManager extends SimplePreparableReloadListener<Map<ResourceLocation, int[]>> {

    private static final Map<ResourceLocation, int[]> COLORMAPS = new HashMap<>();
    protected static int[] EMPTY = new int[]{};

    public static int[] getPixels(ResourceLocation colormap) {
        return COLORMAPS.getOrDefault(colormap, EMPTY);
    }

    /**
     * Performs any reloading that can be done off-thread, such as file IO
     */
    @Override
    protected Map<ResourceLocation, int[]> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, int[]> map = new HashMap<>();
        for (var e : resourceManager.listResources("textures/colormap/polytone", r -> r.getPath().endsWith(".png")).entrySet()) {
            ResourceLocation path = e.getKey();
            try {
                map.put(path, LegacyStuffWrapper.getPixels(resourceManager, path));
            } catch (IOException var4) {
                throw new IllegalStateException("Failed to load colormap texture at " + path, var4);
            }
        }
        return map;
    }

    @Override
    protected void apply(Map<ResourceLocation, int[]> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        COLORMAPS.clear();
        for (var e : object.entrySet()) {
            if (e.getValue().length != 64 * 64) {
                throw new IllegalStateException("Colormap at location " + e.getKey() + " had invalid dimensions. Expected 64x64");
            }else COLORMAPS.put(e.getKey(), e.getValue());
        }
    }
}
