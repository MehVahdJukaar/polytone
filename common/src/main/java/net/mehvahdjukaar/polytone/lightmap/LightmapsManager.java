package net.mehvahdjukaar.polytone.lightmap;

import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class LightmapsManager {

    private static final Map<ResourceLocation, Lightmap> LIGHTMAPS = new HashMap<>();

    public static void process(Map<ResourceLocation, ArrayImage> lightmaps) {


        for(var e : lightmaps.entrySet()){

        }
    }
}
