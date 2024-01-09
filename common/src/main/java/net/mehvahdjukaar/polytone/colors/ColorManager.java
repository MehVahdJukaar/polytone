package net.mehvahdjukaar.polytone.colors;

import com.google.common.collect.Lists;
import com.google.gson.JsonParseException;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.particles.ParticleModifier;
import net.mehvahdjukaar.polytone.utils.SinglePropertiesReloadListener;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class ColorManager extends SinglePropertiesReloadListener {

    private final Map<MapColor, Integer> vanillaMapColors = new HashMap<>();
    private final Map<DyeColor, Integer> vanillaFireworkColors = new EnumMap<>(DyeColor.class);
    private final Map<DyeColor, Integer> vanillaDiffuseColors = new EnumMap<>(DyeColor.class);
    private final Map<DyeColor, Integer> vanillaTextColors = new EnumMap<>(DyeColor.class);


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
                    if (e.getKey() instanceof String key) {
                        String[] split = key.split("/.");
                        parseAllColors(split, e.getValue());
                    }
                }
            }
        } catch (Exception ex) {
            Polytone.LOGGER.error("Visual Properties failed to apply custom MapColors. Rolling back to vanilla state", ex);
            resetValues();
        }
    }


    private void parseAllColors(String[] prop, Object obj) {
        if (is(prop, 0, "map")) {
            String name = get(prop, 1);
            MapColor color = MapColorHelper.byName(name);
            if (color != null) {
                int col = parseHex(obj);
                // save vanilla value
                if (!vanillaMapColors.containsKey(color)) {
                    vanillaMapColors.put(color, color.col);
                }
                color.col = col;

            } else Polytone.LOGGER.error("Unknown MapColor with name {}", name);
        } else if (is(prop, 0, "dye")) {
            String name = get(prop, 1);
            DyeColor color = DyeColor.byName(name, null);
            if (color != null) {
                String param = get(prop, 2);
                if (param == null || param.equals("diffuse")) {
                    //apply diffuse
                    int col = parseHex(obj);
                    // save vanilla value
                    if (!vanillaDiffuseColors.containsKey(color)) {
                        vanillaDiffuseColors.put(color, pack(color.textureDiffuseColors));
                    }
                    color.textureDiffuseColors = unpack(col);
                } else if (param.equals("firework")) {
                    //apply diffuse
                    int col = parseHex(obj);
                    // save vanilla value
                    if (!vanillaFireworkColors.containsKey(color)) {
                        vanillaFireworkColors.put(color, color.fireworkColor);
                    }
                    color.fireworkColor = col;
                } else if (param.equals("text")) {
                    //apply diffuse
                    int col = parseHex(obj);
                    // save vanilla value
                    if (!vanillaTextColors.containsKey(color)) {
                        vanillaTextColors.put(color, color.textColor);
                    }
                    color.textColor = col;
                }
            } else Polytone.LOGGER.error("Unknown DyeColor with name {}", name);
        } else if (is(prop, 0, "particle")) {
            if (is(prop, 1, "portal")) {

            } else Polytone.LOGGER.error("Unknown Particle Color with name {}", get(prop, 1));
        }

    }

    public static int pack(float[] components) {
        int n = (int) (components[0] * 255.0F) << 16;
        int o = (int) (components[1] * 255.0F) << 8;
        int p = (int) (components[2] * 255.0F);
        return (n & 0xFF0000) | (o & 0xFF00) | (p & 0xFF);
    }

    public static float[] unpack(int value) {
        int n = (value & 16711680) >> 16;
        int o = (value & '\uff00') >> 8;
        int p = (value & 255);
        return new float[]{n / 255.0F, o / 255.0F, p / 255.0F};
    }

    private boolean is(String[] array, int index, String value) {
        if (array.length < index) return false;
        return array[index].equals(value);
    }

    @Nullable
    private String get(String[] array, int index) {
        if (array.length < index) return null;
        return array[index];
    }

    @Nullable
    private <T> T get(String[] array, int index, Function<String, T> fun) {
        if (array.length < index) return null;
        return fun.apply(array[index]);
    }


    private static int parseHex(Object obj) {
        if (obj instanceof String value) {
            value = value.replace("#", "").replace("0x", "");
            return Integer.parseInt(value, 16);
        }
        throw new JsonParseException("Failed to parse object " + obj + ". Expected a String");
    }

    private static Supplier<Integer> parseParticleColor(Object obj){
        if (obj instanceof String value) {

        }
        return null;
    }

    private void resetValues() {
        // map colors
        for (var e : vanillaMapColors.entrySet()) {
            MapColor color = e.getKey();
            color.col = e.getValue();
        }
        vanillaMapColors.clear();

        // dye colors
        for (var e : vanillaDiffuseColors.entrySet()) {
            DyeColor color = e.getKey();
            color.textureDiffuseColors = unpack(e.getValue());
        }
        vanillaDiffuseColors.clear();

        for (var e : vanillaFireworkColors.entrySet()) {
            DyeColor color = e.getKey();
            color.fireworkColor = e.getValue();
        }
        vanillaFireworkColors.clear();

        for (var e : vanillaTextColors.entrySet()) {
            DyeColor color = e.getKey();
            color.textColor = e.getValue();
        }
        vanillaTextColors.clear();
    }


}
