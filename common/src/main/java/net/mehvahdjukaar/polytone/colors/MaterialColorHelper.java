package net.mehvahdjukaar.polytone.colors;

import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.world.level.material.MaterialColor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class MaterialColorHelper {

    public static final Codec<MaterialColor> CODEC = Codec.STRING.xmap(MaterialColorHelper::byName, MaterialColor -> "none");

    private static final Map<String, MaterialColor> colorNames = Util.make(() -> {
        Map<String, MaterialColor> map = new HashMap<>();
        map.put("none", MaterialColor.NONE);
        map.put("air", MaterialColor.NONE); //OF
        map.put("grass", MaterialColor.GRASS);
        map.put("sand", MaterialColor.SAND);
        map.put("wool", MaterialColor.WOOL);
        map.put("cloth", MaterialColor.WOOL); //OF
        map.put("fire", MaterialColor.FIRE);
        map.put("tnt", MaterialColor.FIRE); //OF
        map.put("ice", MaterialColor.ICE);
        map.put("metal", MaterialColor.METAL);
        map.put("iron", MaterialColor.METAL); //OF
        map.put("plant", MaterialColor.PLANT);
        map.put("foliage", MaterialColor.PLANT); //OF
        map.put("snow", MaterialColor.SNOW);
        map.put("white", MaterialColor.SNOW); //OF
        map.put("color_white", MaterialColor.SNOW); //NEW
        map.put("clay", MaterialColor.CLAY);
        map.put("dirt", MaterialColor.DIRT);
        map.put("stone", MaterialColor.STONE);
        map.put("water", MaterialColor.WATER);
        map.put("wood", MaterialColor.WOOD);
        map.put("quartz", MaterialColor.QUARTZ);
        map.put("color_orange", MaterialColor.COLOR_ORANGE);
        map.put("orange", MaterialColor.COLOR_ORANGE); //OF
        map.put("color_magenta", MaterialColor.COLOR_MAGENTA);
        map.put("magenta", MaterialColor.COLOR_MAGENTA);
        map.put("color_light_blue", MaterialColor.COLOR_LIGHT_BLUE);
        map.put("light_blue", MaterialColor.COLOR_LIGHT_BLUE); //OF
        map.put("color_yellow", MaterialColor.COLOR_YELLOW);
        map.put("yellow", MaterialColor.COLOR_YELLOW); //OF
        map.put("color_light_green", MaterialColor.COLOR_LIGHT_GREEN);
        map.put("light_green", MaterialColor.COLOR_LIGHT_GREEN); //OF
        map.put("color_pink", MaterialColor.COLOR_PINK);
        map.put("pink", MaterialColor.COLOR_PINK); //OF
        map.put("color_gray", MaterialColor.COLOR_GRAY);
        map.put("gray", MaterialColor.COLOR_GRAY); //OF
        map.put("color_light_gray", MaterialColor.COLOR_LIGHT_GRAY);
        map.put("light_gray", MaterialColor.COLOR_LIGHT_GRAY); //OF
        map.put("color_cyan", MaterialColor.COLOR_CYAN);
        map.put("cyan", MaterialColor.COLOR_CYAN); //OF
        map.put("color_purple", MaterialColor.COLOR_PURPLE);
        map.put("purple", MaterialColor.COLOR_PURPLE); //OF
        map.put("color_blue", MaterialColor.COLOR_BLUE);
        map.put("blue", MaterialColor.COLOR_BLUE); //OF
        map.put("color_brown", MaterialColor.COLOR_BROWN);
        map.put("brown", MaterialColor.COLOR_BROWN); //OF
        map.put("color_green", MaterialColor.COLOR_GREEN);
        map.put("green", MaterialColor.COLOR_GREEN); //OF
        map.put("color_red", MaterialColor.COLOR_RED);
        map.put("red", MaterialColor.COLOR_RED); //OF
        map.put("color_black", MaterialColor.COLOR_BLACK);
        map.put("black", MaterialColor.COLOR_BLACK); //OF
        map.put("obsidian", MaterialColor.COLOR_BLACK); //OF
        map.put("gold", MaterialColor.GOLD);
        map.put("diamond", MaterialColor.DIAMOND);
        map.put("lapis", MaterialColor.LAPIS);
        map.put("emerald", MaterialColor.EMERALD);
        map.put("podzol", MaterialColor.PODZOL);
        map.put("nether", MaterialColor.NETHER); //obsidian, netherrack?
        map.put("netherrack", MaterialColor.NETHER); //OF
        map.put("white_terracotta", MaterialColor.TERRACOTTA_WHITE); //OF
        map.put("terracotta_white", MaterialColor.TERRACOTTA_WHITE);
        map.put("orange_terracotta", MaterialColor.TERRACOTTA_ORANGE); //OF
        map.put("terracotta_orange", MaterialColor.TERRACOTTA_ORANGE);
        map.put("magenta_terracotta", MaterialColor.TERRACOTTA_MAGENTA); //OF
        map.put("terracotta_magenta", MaterialColor.TERRACOTTA_MAGENTA);
        map.put("light_blue_terracotta", MaterialColor.TERRACOTTA_LIGHT_BLUE); //OF
        map.put("terracotta_light_blue", MaterialColor.TERRACOTTA_LIGHT_BLUE);
        map.put("yellow_terracotta", MaterialColor.TERRACOTTA_YELLOW); //OF
        map.put("terracotta_yellow", MaterialColor.TERRACOTTA_YELLOW);
        map.put("light_green_terracotta", MaterialColor.TERRACOTTA_LIGHT_GREEN); //OF
        map.put("terracotta_light_green", MaterialColor.TERRACOTTA_LIGHT_GREEN);
        map.put("pink_terracotta", MaterialColor.TERRACOTTA_PINK); //OF
        map.put("terracotta_pink", MaterialColor.TERRACOTTA_PINK);
        map.put("gray_terracotta", MaterialColor.TERRACOTTA_GRAY); //OF
        map.put("terracotta_gray", MaterialColor.TERRACOTTA_GRAY);
        map.put("light_gray_terracotta", MaterialColor.TERRACOTTA_LIGHT_GRAY); //OF
        map.put("terracotta_light_gray", MaterialColor.TERRACOTTA_LIGHT_GRAY);
        map.put("cyan_terracotta", MaterialColor.TERRACOTTA_CYAN); //OF
        map.put("terracotta_cyan", MaterialColor.TERRACOTTA_CYAN);
        map.put("purple_terracotta", MaterialColor.TERRACOTTA_PURPLE); //OF
        map.put("terracotta_purple", MaterialColor.TERRACOTTA_PURPLE);
        map.put("blue_terracotta", MaterialColor.TERRACOTTA_BLUE); //OF
        map.put("terracotta_blue", MaterialColor.TERRACOTTA_BLUE);
        map.put("brown_terracotta", MaterialColor.TERRACOTTA_BROWN); //OF
        map.put("terracotta_brown", MaterialColor.TERRACOTTA_BROWN);
        map.put("green_terracotta", MaterialColor.TERRACOTTA_GREEN); //OF
        map.put("terracotta_green", MaterialColor.TERRACOTTA_GREEN);
        map.put("red_terracotta", MaterialColor.TERRACOTTA_RED); //OF
        map.put("terracotta_red", MaterialColor.TERRACOTTA_RED);
        map.put("black_terracotta", MaterialColor.TERRACOTTA_BLACK); //OF
        map.put("terracotta_black", MaterialColor.TERRACOTTA_BLACK);
        map.put("crimson_nylium", MaterialColor.CRIMSON_NYLIUM);
        map.put("crimson_stem", MaterialColor.CRIMSON_STEM);
        map.put("crimson_hyphae", MaterialColor.CRIMSON_HYPHAE);
        map.put("warped_nylium", MaterialColor.WARPED_NYLIUM);
        map.put("warped_stem", MaterialColor.WARPED_STEM);
        map.put("warped_hyphae", MaterialColor.WARPED_HYPHAE);
        map.put("warped_wart_block", MaterialColor.WARPED_WART_BLOCK);
        map.put("deepslate", MaterialColor.DEEPSLATE);
        map.put("raw_iron", MaterialColor.RAW_IRON);
        map.put("glow_lichen", MaterialColor.GLOW_LICHEN);

        return map;
    });

    @Nullable
    public static MaterialColor byName(String colorName) {
        return colorNames.get(colorName);
    }
}
