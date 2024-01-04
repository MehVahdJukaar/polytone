package net.mehvahdjukaar.polytone;

import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class MapColorHelper {

    public static final Codec<MapColor> CODEC = Codec.STRING.xmap(MapColorHelper::byName, mapColor -> "none");

    private static final Map<String, MapColor> colorNames = Util.make(() -> {
        Map<String, MapColor> map = new HashMap<>();
        map.put("none", MapColor.NONE);
        map.put("air", MapColor.NONE); //OF
        map.put("grass", MapColor.GRASS);
        map.put("sand", MapColor.SAND);
        map.put("wool", MapColor.WOOL);
        map.put("cloth", MapColor.WOOL); //OF
        map.put("fire", MapColor.FIRE);
        map.put("tnt", MapColor.FIRE); //OF
        map.put("ice", MapColor.ICE);
        map.put("metal", MapColor.METAL);
        map.put("iron", MapColor.METAL); //OF
        map.put("plant", MapColor.PLANT);
        map.put("foliage", MapColor.PLANT); //OF
        map.put("snow", MapColor.SNOW);
        map.put("white", MapColor.SNOW); //OF
        map.put("color_white", MapColor.SNOW); //NEW
        map.put("clay", MapColor.CLAY);
        map.put("dirt", MapColor.DIRT);
        map.put("stone", MapColor.STONE);
        map.put("water", MapColor.WATER);
        map.put("wood", MapColor.WOOD);
        map.put("quartz", MapColor.QUARTZ);
        map.put("color_orange", MapColor.COLOR_ORANGE);
        map.put("orange", MapColor.COLOR_ORANGE); //OF
        map.put("color_magenta", MapColor.COLOR_MAGENTA);
        map.put("magenta", MapColor.COLOR_MAGENTA);
        map.put("color_light_blue", MapColor.COLOR_LIGHT_BLUE);
        map.put("light_blue", MapColor.COLOR_LIGHT_BLUE); //OF
        map.put("color_yellow", MapColor.COLOR_YELLOW);
        map.put("yellow", MapColor.COLOR_YELLOW); //OF
        map.put("color_light_green", MapColor.COLOR_LIGHT_GREEN);
        map.put("light_green", MapColor.COLOR_LIGHT_GREEN); //OF
        map.put("color_pink", MapColor.COLOR_PINK);
        map.put("pink", MapColor.COLOR_PINK);
        map.put("color_gray", MapColor.COLOR_GRAY);
        map.put("color_light_gray", MapColor.COLOR_LIGHT_GRAY);
        map.put("light_gray", MapColor.COLOR_LIGHT_GRAY); //OF
        map.put("color_cyan", MapColor.COLOR_CYAN);
        map.put("cyan", MapColor.COLOR_CYAN); //OF
        map.put("color_purple", MapColor.COLOR_PURPLE);
        map.put("purple", MapColor.COLOR_PURPLE); //OF
        map.put("color_blue", MapColor.COLOR_BLUE);
        map.put("blue", MapColor.COLOR_BLUE); //OF
        map.put("color_brown", MapColor.COLOR_BROWN);
        map.put("brown", MapColor.COLOR_BROWN); //OF
        map.put("color_green", MapColor.COLOR_GREEN);
        map.put("green", MapColor.COLOR_GREEN); //OF
        map.put("color_red", MapColor.COLOR_RED);
        map.put("red", MapColor.COLOR_RED); //OF
        map.put("color_black", MapColor.COLOR_BLACK);
        map.put("black", MapColor.COLOR_BLACK); //OF
        map.put("obsidian", MapColor.COLOR_BLACK); //OF
        map.put("gold", MapColor.GOLD);
        map.put("diamond", MapColor.DIAMOND);
        map.put("lapis", MapColor.LAPIS);
        map.put("emerald", MapColor.EMERALD);
        map.put("podzol", MapColor.PODZOL);
        map.put("nether", MapColor.NETHER); //obsidian, netherrack?
        map.put("netherrack", MapColor.NETHER); //OF
        map.put("white_terracotta", MapColor.TERRACOTTA_WHITE); //OF
        map.put("terracotta_white", MapColor.TERRACOTTA_WHITE);
        map.put("orange_terracotta", MapColor.TERRACOTTA_ORANGE); //OF
        map.put("terracotta_orange", MapColor.TERRACOTTA_ORANGE);
        map.put("magenta_terracotta", MapColor.TERRACOTTA_MAGENTA); //OF
        map.put("terracotta_magenta", MapColor.TERRACOTTA_MAGENTA);
        map.put("light_blue_terracotta", MapColor.TERRACOTTA_LIGHT_BLUE); //OF
        map.put("terracotta_light_blue", MapColor.TERRACOTTA_LIGHT_BLUE);
        map.put("yellow_terracotta", MapColor.TERRACOTTA_YELLOW); //OF
        map.put("terracotta_yellow", MapColor.TERRACOTTA_YELLOW);
        map.put("light_green_terracotta", MapColor.TERRACOTTA_LIGHT_GREEN); //OF
        map.put("terracotta_light_green", MapColor.TERRACOTTA_LIGHT_GREEN);
        map.put("pink_terracotta", MapColor.TERRACOTTA_PINK); //OF
        map.put("terracotta_pink", MapColor.TERRACOTTA_PINK);
        map.put("gray_terracotta", MapColor.TERRACOTTA_GRAY); //OF
        map.put("terracotta_gray", MapColor.TERRACOTTA_GRAY);
        map.put("light_gray_terracotta", MapColor.TERRACOTTA_LIGHT_GRAY); //OF
        map.put("terracotta_light_gray", MapColor.TERRACOTTA_LIGHT_GRAY);
        map.put("cyan_terracotta", MapColor.TERRACOTTA_CYAN); //OF
        map.put("terracotta_cyan", MapColor.TERRACOTTA_CYAN);
        map.put("purple_terracotta", MapColor.TERRACOTTA_PURPLE); //OF
        map.put("terracotta_purple", MapColor.TERRACOTTA_PURPLE);
        map.put("blue_terracotta", MapColor.TERRACOTTA_BLUE); //OF
        map.put("terracotta_blue", MapColor.TERRACOTTA_BLUE);
        map.put("brown_terracotta", MapColor.TERRACOTTA_BROWN); //OF
        map.put("terracotta_brown", MapColor.TERRACOTTA_BROWN);
        map.put("green_terracotta", MapColor.TERRACOTTA_GREEN); //OF
        map.put("terracotta_green", MapColor.TERRACOTTA_GREEN);
        map.put("red_terracotta", MapColor.TERRACOTTA_RED); //OF
        map.put("terracotta_red", MapColor.TERRACOTTA_RED);
        map.put("black_terracotta", MapColor.TERRACOTTA_BLACK); //OF
        map.put("terracotta_black", MapColor.TERRACOTTA_BLACK);
        map.put("crimson_nylium", MapColor.CRIMSON_NYLIUM);
        map.put("crimson_stem", MapColor.CRIMSON_STEM);
        map.put("crimson_hyphae", MapColor.CRIMSON_HYPHAE);
        map.put("warped_nylium", MapColor.WARPED_NYLIUM);
        map.put("warped_stem", MapColor.WARPED_STEM);
        map.put("warped_hyphae", MapColor.WARPED_HYPHAE);
        map.put("warped_wart_block", MapColor.WARPED_WART_BLOCK);
        map.put("deepslate", MapColor.DEEPSLATE);
        map.put("raw_iron", MapColor.RAW_IRON);
        map.put("glow_lichen", MapColor.GLOW_LICHEN);

        return map;
    });

    @Nullable
    public static MapColor byName(String colorName) {
        return colorNames.get(colorName);
    }
}
