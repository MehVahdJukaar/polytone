package net.mehvahdjukaar.polytone.lightmap;

import com.mojang.blaze3d.platform.NativeImage;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.PartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class LightmapsManager extends PartialReloader<Map<ResourceLocation, ArrayImage>> {

    public static final ResourceLocation GUI_LIGHTMAP = Polytone.res("lightmaps/gui.png");

    private final Map<ResourceLocation, Lightmap> lightmaps = new HashMap<>();
    private ResourceKey<Level> lastDimension = null;
    private Lightmap currentLightmap = null;

    public LightmapsManager() {
        super("lightmaps");
    }

    @Override
    protected Map<ResourceLocation, ArrayImage> prepare(ResourceManager resourceManager) {
        return ArrayImage.gatherImages(resourceManager, path());
    }

    @Override
    public void process(Map<ResourceLocation, ArrayImage> images) {
        lastDimension = null;
        currentLightmap = null;

        Map<ResourceLocation, Map<String, ArrayImage>> grouped = new HashMap<>();
        for (var e : images.entrySet()) {
            ArrayImage value = e.getValue();
            int height = value.height();
            ResourceLocation location = e.getKey();
            if (height != 16 && height != 32 && height != 64) {
                throw new IllegalStateException("Lightmap must be either 16, 32 or 64 pixels tall. Provided one at " + location + " was " + height + " pixels");
            } else {
                String path = location.getPath();
                if (path.endsWith("_thunder")) {
                    grouped.computeIfAbsent(location.withPath(path.replace("_thunder", "")),
                            g -> new HashMap<>()).put("thunder", value);
                } else if (path.endsWith("_rain")) {
                    grouped.computeIfAbsent(location.withPath(path.replace("_rain", "")),
                            g -> new HashMap<>()).put("rain", value);
                } else {
                    grouped.computeIfAbsent(location, g -> new HashMap<>()).put("normal", value);
                }
            }
        }

        for (var e : grouped.entrySet()) {
            ResourceLocation location = e.getKey();
            var map = e.getValue();
            Lightmap lightmap = new Lightmap(map.get("normal"), map.get("rain"), map.get("thunder"));

            ResourceLocation localId = Polytone.getLocalId(location);
            lightmaps.put(localId, lightmap);
            lightmaps.put(location, lightmap);
        }
    }

    @Override
    protected void reset() {
        lightmaps.clear();
    }

    public boolean maybeModifyLightTexture(LightTexture instance,
                                                  NativeImage lightPixels,
                                                  DynamicTexture lightTexture,
                                                  Minecraft minecraft, ClientLevel level,
                                                  float flicker, float partialTicks) {
        if (lastDimension != level.dimension()) {
            lastDimension = level.dimension();
            currentLightmap = lightmaps.get(lastDimension.location());
        }
        if (currentLightmap != null && !hack) {
            currentLightmap.applyToLightTexture(instance, lightPixels, lightTexture, minecraft,
                    level, flicker, partialTicks);
            return true;
        }
        return false;
    }

    private static boolean hack = false;

    public static void setupForGUI(boolean gui) {
        hack = gui;
    }
}
