package net.mehvahdjukaar.polytone.color;

import com.google.common.collect.Lists;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.particle.ParticleModifiersManager;
import net.mehvahdjukaar.polytone.utils.SinglePropertiesReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

public class ColorManager extends SinglePropertiesReloadListener {

    private final Object2IntMap<MapColor> vanillaMapColors = new Object2IntOpenHashMap<>();
    private final Map<DyeColor, Integer> vanillaFireworkColors = new EnumMap<>(DyeColor.class);
    private final Map<DyeColor, Integer> vanillaDiffuseColors = new EnumMap<>(DyeColor.class);
    private final Map<DyeColor, Integer> vanillaTextColors = new EnumMap<>(DyeColor.class);
    private final Object2IntMap<SpawnEggItem> vanillaEggsBackgrounds = new Object2IntOpenHashMap<>();
    private final Object2IntMap<SpawnEggItem> vanillaEggsHighlight = new Object2IntOpenHashMap<>();
    private final Object2IntMap<MobEffect> vanillaEffectColors = new Object2IntOpenHashMap<>();

    private final Map<DyeColor, Integer> customSheepColors = new EnumMap<>(DyeColor.class);

    public ColorManager() {
        super("color.properties", "optifine", "vanadium", "colormatic", Polytone.MOD_ID);
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
                        String[] split = key.split("\\.");
                        parseColor(split, e.getValue());
                    }
                }
            }
        } catch (Exception ex) {
            Polytone.LOGGER.error("Visual Properties failed to apply custom MapColors. Rolling back to vanilla state", ex);
            resetValues();
        }

        regenSheepColors();
    }


    private void parseColor(String[] prop, Object obj) {
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

            } else Polytone.LOGGER.warn("Unknown MapColor with name {}", name);
        } else if (is(prop, 0, "dye")) {
            String name = get(prop, 1);
            DyeColor color = DyeColor.byName(name, null);
            if (color != null) {
                String param = get(prop, 2);
                int col = parseHex(obj);
                if (param == null || param.equals("diffuse")) {
                    // save vanilla value
                    if (!vanillaDiffuseColors.containsKey(color)) {
                        vanillaDiffuseColors.put(color, pack(color.textureDiffuseColors));
                    }
                    color.textureDiffuseColors = unpack(col);
                } else if (param.equals("firework")) {
                    // save vanilla value
                    if (!vanillaFireworkColors.containsKey(color)) {
                        vanillaFireworkColors.put(color, color.fireworkColor);
                    }
                    color.fireworkColor = col;
                } else if (param.equals("text")) {
                    // save vanilla value
                    if (!vanillaTextColors.containsKey(color)) {
                        vanillaTextColors.put(color, color.textColor);
                    }
                    color.textColor = col;
                }
            } else Polytone.LOGGER.warn("Unknown DyeColor with name {}", name);
        } else if (is(prop, 0, "particle")) {
            if (prop.length > 1) {
                ResourceLocation id = new ResourceLocation(prop[1].replace("\\", ""));
                if (obj instanceof String s) {
                    try {
                        // turn from hex to decimal if it is a single number
                        int hex = parseHex(s);
                        ParticleModifiersManager.addCustomParticleColor(id, String.valueOf(hex));
                    } catch (Exception e) {
                        ParticleModifiersManager.addCustomParticleColor(id, s);
                    }
                }
            }

        } else if (is(prop, 0, "egg")) {
            if (prop.length > 2) {
                ResourceLocation id = new ResourceLocation(prop[2].replace("\\", ""));
                Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                if (item instanceof SpawnEggItem spawnEggItem) {
                    int col = parseHex(obj);

                    if (is(prop, 1, "shell")) {
                        if (!vanillaEggsBackgrounds.containsKey(spawnEggItem)) {
                            vanillaEggsBackgrounds.put(spawnEggItem, spawnEggItem.backgroundColor);
                        }
                        spawnEggItem.backgroundColor = col;
                    } else if (is(prop, 1, "spots")) {
                        if (!vanillaEggsHighlight.containsKey(spawnEggItem)) {
                            vanillaEggsHighlight.put(spawnEggItem, spawnEggItem.highlightColor);
                        }
                        spawnEggItem.highlightColor = col;
                    }
                } else Polytone.LOGGER.warn("Unknown or invalid Spawn Egg Item with name {}", id);
            }
        } else if (is(prop, 0, "potion") || is(prop, 0, "effect")) {
            ResourceLocation id = new ResourceLocation(prop[2].replace("\\", ""));
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.getOptional(id).orElse(null);
            if (effect != null) {
                int col = parseHex(obj);
                if (!vanillaEffectColors.containsKey(effect)) {
                    vanillaEffectColors.put(effect, effect.getColor());
                }
                effect.color = col;
            } else Polytone.LOGGER.warn("Unknown Mob Effect with name {}", id);
        } else if (is(prop, 0, "sheep")) {
            String name = get(prop, 1);
            DyeColor color = DyeColor.byName(name, null);
            if (color != null) {
                int col = parseHex(obj);
                customSheepColors.put(color, col);
            } else Polytone.LOGGER.warn("Unknown Dye Color with name {}", name);
        }
    }


    public static int pack(float... components) {
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
        if (array.length <= index) return false;
        return array[index].equals(value);
    }

    @Nullable
    private String get(String[] array, int index) {
        if (array.length <= index) return null;
        return array[index];
    }

    @Nullable
    private <T> T get(String[] array, int index, Function<String, T> fun) {
        if (array.length <= index) return null;
        return fun.apply(array[index]);
    }


    private static int parseHex(Object obj) {
        if (obj instanceof String value) {
            value = value.replace("#", "").replace("0x", "");
            return Integer.parseInt(value, 16);
        }
        throw new JsonParseException("Failed to parse object " + obj + ". Expected a String");
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

        //spawn eggs

        for (var e : vanillaEggsBackgrounds.entrySet()) {
            SpawnEggItem item = e.getKey();
            item.backgroundColor = e.getValue();
        }
        vanillaEggsBackgrounds.clear();

        for (var e : vanillaEggsHighlight.entrySet()) {
            SpawnEggItem item = e.getKey();
            item.highlightColor = e.getValue();
        }
        vanillaEggsHighlight.clear();
    }

    public void regenSheepColors() {
        Sheep.COLORARRAY_BY_COLOR = new EnumMap<>(DyeColor.class);
        for (var d : DyeColor.values()) {
            Sheep.COLORARRAY_BY_COLOR.put(d, Sheep.createSheepColor(d));
        }
        for (var e : customSheepColors.entrySet()) {
            Sheep.COLORARRAY_BY_COLOR.put(e.getKey(), unpack(e.getValue()));
        }
        customSheepColors.clear();
    }

}
