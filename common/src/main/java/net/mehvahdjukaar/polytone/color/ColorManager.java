package net.mehvahdjukaar.polytone.color;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DynamicOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.mixins.accessor.DustParticleOptionAccessor;
import net.mehvahdjukaar.polytone.mixins.accessor.SheepAccessor;
import net.mehvahdjukaar.polytone.block.BlockContextExpression;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.SingleJsonOrPropertiesReloadListener;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Function;

public class ColorManager extends SingleJsonOrPropertiesReloadListener {

    private final Object2IntMap<MapColor> vanillaMapColors = new Object2IntOpenHashMap<>();
    private final Map<DyeColor, Integer> vanillaFireworkColors = new EnumMap<>(DyeColor.class);
    private final Map<DyeColor, Integer> vanillaDiffuseColors = new EnumMap<>(DyeColor.class);
    private final Map<DyeColor, Integer> vanillaTextColors = new EnumMap<>(DyeColor.class);
    private final Map<ChatFormatting, Integer> vanillaChatFormatting = new EnumMap<>(ChatFormatting.class);
    private final Object2IntMap<SpawnEggItem> vanillaEggsBackgrounds = new Object2IntOpenHashMap<>();
    private final Object2IntMap<SpawnEggItem> vanillaEggsHighlight = new Object2IntOpenHashMap<>();
    private final Object2IntMap<MobEffect> vanillaEffectColors = new Object2IntOpenHashMap<>();

    private final Map<DyeColor, Integer> customSheepColors = new EnumMap<>(DyeColor.class);
    protected final List<Vec3> originalRedstoneWireColors = Arrays.stream(RedStoneWireBlock.COLORS).toList();

    @Nullable
    private BlockContextExpression xpOrbColor;
    @Nullable
    private BlockContextExpression xpOrbColorR;
    @Nullable
    private BlockContextExpression xpOrbColorG;
    @Nullable
    private BlockContextExpression xpOrbColorB;

    private int xpBar = 8453920;

    public ColorManager() {
        //determines the priority. last applied will be the one with highest priority. Polytone is last applied one
        super("color.properties", "colors.json",
                Polytone.MOD_ID, "colormatic", "vanadium", "optifine");
    }

    public int getXpBar() {
        return xpBar;
    }


    @Override
    protected void process(Map<ResourceLocation, Properties> map, DynamicOps<JsonElement> ops) {
        //iterate from the lowest priority to highest
        var keySet = new ArrayList<>(map.keySet());
        Lists.reverse(keySet);
        for (var k : keySet) {
            Properties p = map.get(k);
            for (var e : p.entrySet()) {
                if (e.getKey() instanceof String key) {
                    String[] split = key.split("\\.");
                    try {
                        parseColor(split, e.getValue(), k);
                    } catch (Exception e1) {
                        Polytone.LOGGER.error("Failed to parse color property {} in file {}", key, k);
                    }
                }
            }
        }

        regenSheepColors();
    }


    private void parseColor(String[] prop, Object obj, ResourceLocation colorPropFileId) {
        if (!(obj instanceof String str)) return;
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
                        vanillaDiffuseColors.put(color, color.getTextureDiffuseColor());
                    }
                    color.textureDiffuseColor = FastColor.ARGB32.opaque(col);
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
                String s = prop[1];
                ResourceLocation id = ResourceLocation.tryParse(s.replace("\\", ""));
                try {
                    // turn from hex to decimal if it is a single number
                    int hex = parseHex(str);
                    Polytone.PARTICLE_MODIFIERS.addCustomParticleColor(id, String.valueOf(hex));
                } catch (Exception e) {
                    Polytone.PARTICLE_MODIFIERS.addCustomParticleColor(id, str);
                }
            }
        } else if (is(prop, 0, "egg")) {
            if (prop.length > 2) {
                ResourceLocation id = ResourceLocation.tryParse(prop[2].replace("\\", ""));
                Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                if (item == null) {
                    var entity = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
                    if (entity != null) {
                        item = SpawnEggItem.byId(entity);
                    }
                }
                if (item == null) {
                    item = BuiltInRegistries.ITEM.getOptional(id.withSuffix("_spawn_egg")).orElse(null);
                }
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
                } else{
                    Polytone.LOGGER.warn("Unknown or invalid Spawn Egg Item with name {}", id);
                }
            }
        } else if (is(prop, 0, "potion") || is(prop, 0, "effect")) {
            ResourceLocation id = ResourceLocation.parse(prop[1].replace("\\", ""));
            int col = parseHex(obj);
            if (id.getPath().equals("empty")) {
                //TODO:
               // PotionContents.EMPTY_COLOR = col;
            } else if (id.getPath().equals("water")) {
                PotionContents.BASE_POTION_COLOR = col;
            } else {
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.getOptional(id).orElse(null);
                if (effect != null) {
                    if (!vanillaEffectColors.containsKey(effect)) {
                        vanillaEffectColors.put(effect, effect.getColor());
                    }
                    effect.color = col;
                } else Polytone.LOGGER.warn("Unknown Mob Effect with name {}", id);
            }
        } else if (is(prop, 0, "sheep")) {
            String name = get(prop, 1);
            DyeColor color = DyeColor.byName(name, null);
            if (color != null) {
                int col = parseHex(obj);
                customSheepColors.put(color, col);
            } else Polytone.LOGGER.warn("Unknown Dye Color with name {}", name);
        } else if (is(prop, 0, "xporb")) {
            if (is(prop, 1, "color")) {
                xpOrbColor = new BlockContextExpression(str);
            } else if (is(prop, 1, "red")) {
                xpOrbColorR = new BlockContextExpression(str);
            } else if (is(prop, 1, "green")) {
                xpOrbColorG = new BlockContextExpression(str);
            } else if (is(prop, 1, "blue")) {
                xpOrbColorB = new BlockContextExpression(str);
            }

        } else if (is(prop, 0, "redstone")) {
            String ind = get(prop, 1);
            if (ind != null) {
                int code = Integer.parseInt(ind);
                if (code < RedStoneWireBlock.COLORS.length) {
                    int col = parseHex(obj);
                    var rgb = ColorUtils.unpack(col);
                    RedStoneWireBlock.COLORS[code] = new Vec3(rgb[0], rgb[1], rgb[2]);
                    if (code == 15) {
                        Vector3f maxPower = new Vector3f(rgb[0], rgb[1], rgb[2]);
                        net.minecraft.core.particles.DustParticleOptions.REDSTONE_PARTICLE_COLOR = maxPower;
                        ((DustParticleOptionAccessor) DustParticleOptions.REDSTONE).setColor(maxPower);
                    }
                } else Polytone.LOGGER.warn("Redstone color index must be between 0 and 15");
            }
        } else if (is(prop, 0, "text")) {
            int col = parseHex(obj);
            ChatFormatting text = null;
            if (is(prop, 1, "xpbar")) {
                xpBar = col;
            } else if (is(prop, 1, "code")) {
                String s = get(prop, 2);
                if (s != null) {
                    int code = Integer.parseInt(s);
                    text = ChatFormatting.getById(code);
                }
            } else {
                String s = get(prop, 1);
                text = ChatFormatting.getByName(s);
            }
            if (text != null) {
                if (!vanillaChatFormatting.containsKey(text)) {
                    vanillaChatFormatting.put(text, text.getColor());
                }
                text.color = col;
                TextColor tc = TextColor.fromLegacyFormat(text);
                tc.value = col;
            }
        } else if (is(prop, 0, "palette")) {
            if (is(prop, 1, "block")) {
                if (prop.length > 2 && obj instanceof String) {
                    String path = prop[2].replace("~/colormap/", colorPropFileId.getNamespace() + ":");
                    Polytone.BLOCK_MODIFIERS.addSimpleColormap(ResourceLocation.tryParse(path), str);
                }
            }
        }
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
            return Integer.parseInt(value.trim(), 16);
        }
        throw new JsonParseException("Failed to parse object " + obj + ". Expected a String");
    }

    @Override
    public void reset() {
        //TODO:
        //PotionContents.EMPTY_COLOR = 16253176;
        PotionContents.BASE_POTION_COLOR = 3694022;
        xpBar = 8453920;
        xpOrbColor = null;
        xpOrbColorR = null;
        xpOrbColorG = null;
        xpOrbColorB = null;
        // map colors
        for (var e : vanillaMapColors.entrySet()) {
            MapColor color = e.getKey();
            color.col = e.getValue();
        }
        vanillaMapColors.clear();

        // dye colors
        for (var e : vanillaDiffuseColors.entrySet()) {
            DyeColor color = e.getKey();
            color.textureDiffuseColor = e.getValue();
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

        //chat formatting
        for (var e : vanillaChatFormatting.entrySet()) {
            ChatFormatting text = e.getKey();
            text.color = e.getValue();
            TextColor tc = TextColor.fromLegacyFormat(text);
            tc.value = e.getValue();
        }

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

        RedStoneWireBlock.COLORS = originalRedstoneWireColors.toArray(new Vec3[0]);
        DustParticleOptions.REDSTONE_PARTICLE_COLOR = new Vector3f(1, 0, 0);//default
        ((DustParticleOptionAccessor) DustParticleOptions.REDSTONE).setColor(DustParticleOptions.REDSTONE_PARTICLE_COLOR);
    }

    public void regenSheepColors() {
        Sheep.COLOR_BY_DYE = new EnumMap<>(DyeColor.class);
        for (var d : DyeColor.values()) {

            Sheep.COLOR_BY_DYE.put(d, SheepAccessor.invokeCreateSheepColor(d));
        }
        Sheep.COLOR_BY_DYE.putAll(customSheepColors);
        customSheepColors.clear();
    }

    @Nullable
    public float[] getXpOrbColor(ExperienceOrb orb, float partialTicks) {
        if (xpOrbColor != null) {
            int color = (int) xpOrbColor.getValue(orb.position(), orb.tickCount + partialTicks);
            return ColorUtils.unpack(color);
        }
        if (xpOrbColorR == null && xpOrbColorG == null && xpOrbColorB == null) return null;
        float r = 0;
        float g = 0;
        float b = 0;
        if (xpOrbColorR != null) r = (float) xpOrbColorR.getValue(orb.position(), orb.tickCount + partialTicks);
        if (xpOrbColorG != null) g = (float) xpOrbColorG.getValue(orb.position(), orb.tickCount + partialTicks);
        if (xpOrbColorB != null) b = (float) xpOrbColorB.getValue(orb.position(), orb.tickCount + partialTicks);
        return new float[]{r, g, b};
    }

}
