package net.mehvahdjukaar.polytone.utils;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class LegacyHelper {

    private static final Map<String, String> PATHS = Util.make(new Object2ObjectOpenHashMap<>(), m -> {
                m.put("world0", "overworld");
                m.put("world0_thunder", "overworld_thunder");
                m.put("world0_rain", "overworld_rain");
                m.put("world1", "the_end");
                m.put("world-1", "the_nether");
                m.put("pine", "spruce_leaves");
                m.put("birch", "birch_leaves");
                m.put("redstone", "redstone_wire");
                m.put("pumpkinstem", "pumpkin_stem");
                m.put("melonstem", "melon_stem");
                m.put("underwater", "water_fog");
            }
    );

    public static <T> Map<ResourceLocation, T> convertPaths(Map<ResourceLocation, T> map) {
        Map<ResourceLocation, T> toUpdate = new HashMap<>();
        for(var entry : map.entrySet()){
            ResourceLocation id = entry.getKey();
            String path = id.getPath();
            String newPath = PATHS.get(path);
            if(newPath != null){
                toUpdate.put(new ResourceLocation(id.getNamespace(), newPath), entry.getValue());
            }
        }
        map.putAll(toUpdate);
        return map;
    }
}
