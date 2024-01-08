package net.mehvahdjukaar.polytone.colors;

import com.google.common.collect.Lists;
import net.mehvahdjukaar.polytone.utils.SinglePropertiesReloadListener;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.MapColor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ColorManager extends SinglePropertiesReloadListener {

    private final Map<MapColor, Integer> vanillaValues = new HashMap<>();

    public ColorManager() {
        super("optifine/color.properties",
                "vanadium/color.properties",
                "colormatic/color.properties",
                Polytone.MOD_ID + "/color.properties");
    }

    @Override
    protected void apply(List<Properties> list, ResourceManager resourceManager, ProfilerFiller profiler) {
        resetValues();

        //iterate from the lowest priority to highest
        Lists.reverse(list);
        try {
            for (var v : list) {
                for (var e : v.entrySet()) {
                    if (e.getKey() instanceof String colorName) {
                        colorName = colorName.replace("map.", "");
                        MapColor c = MapColorHelper.byName(colorName);
                        if(c != null) {
                            var i = e.getValue();
                            if (i instanceof String value) {
                                value = value.replace("#", "").replace("0x", "");
                                int col = Integer.parseInt(value, 16);
                                // save vanilla value
                                if(!vanillaValues.containsKey(c)){
                                    vanillaValues.put(c, c.col);
                                }
                                c.col = col;
                            }
                        }else{
                            //error
                            int err0r = 1;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Polytone.LOGGER.error("Visual Properties failed to apply custom MapColors. Rolling back to vanilla state", ex);
            resetValues();
        }
    }

    private void resetValues() {
        for (var e : vanillaValues.entrySet()) {
            MapColor color = e.getKey();
            color.col = e.getValue();
        }
        vanillaValues.clear();
    }




}
