package net.mehvahdjukaar.polytone.lightmap;

import com.mojang.blaze3d.platform.NativeImage;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class LightmapsManager {

    private static final Map<ResourceLocation, Lightmap> LIGHTMAPS = new HashMap<>();
    private static ResourceKey<Level> lastDimension = null;
    private static Lightmap currentLightmap = null;

    public static void process(Map<ResourceLocation, ArrayImage> lightmaps) {
        LIGHTMAPS.clear();
        lastDimension = null;
        currentLightmap = null;

        for (var e : lightmaps.entrySet()) {
            ArrayImage value = e.getValue();
            int height = value.height();
            ResourceLocation location = e.getKey();
            if (height != 32 && height != 64) {
                throw new IllegalStateException("Lightmap must be either 32 or 64 pixels tall. Provided one at " + location + " was " + height + " pixels");
            } else {
                //adds both possible locs
                ResourceLocation localId = Polytone.getLocalId(location);
                Lightmap lightmap = new Lightmap(value);
                LIGHTMAPS.put(localId, lightmap);
                LIGHTMAPS.put(location, lightmap);
            }
        }
    }

    public static boolean maybeModifyLightTexture(LightTexture instance,
                                                  NativeImage lightPixels,
                                                  DynamicTexture lightTexture,
                                                  Minecraft minecraft, ClientLevel level,
                                                  float flicker, float partialTicks) {
        if (lastDimension != level.dimension()) {
            lastDimension = level.dimension();
            currentLightmap = LIGHTMAPS.get(lastDimension.location());
        }
        if (currentLightmap != null) {
            currentLightmap.applyToLightTexture(instance, lightPixels, lightTexture, minecraft, level, flicker, partialTicks);
            return true;
        }
        return false;
    }
}
