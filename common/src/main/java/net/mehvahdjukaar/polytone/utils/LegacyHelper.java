package net.mehvahdjukaar.polytone.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.block.BlockPropertyModifier;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.IColormapNumberProvider;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.StemBlock;

import java.util.*;
import java.util.stream.Collectors;

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
        List<ResourceLocation> toRemove = new ArrayList<>();
        for (var entry : map.entrySet()) {
            ResourceLocation id = entry.getKey();
            String path = id.getPath();
            String newPath = PATHS.get(path);
            if (newPath != null) {
                toUpdate.put(id.withPath(newPath), entry.getValue());
                toRemove.add(id);
            }
        }
        toRemove.forEach(map.keySet()::remove);
        map.putAll(toUpdate);
        return map;
    }


    public static Map<ResourceLocation, BlockPropertyModifier> convertBlockProperties(
            Map<ResourceLocation, Properties> ofProperties, Map<ResourceLocation, ArrayImage> textures) {

        List<ResourceLocation> ids = new ArrayList<>();
        ids.addAll(ofProperties.keySet());
        ids.addAll(textures.keySet());

        Map<ResourceLocation, BlockPropertyModifier> map = new HashMap<>();

        for (ResourceLocation id : ids) {
            Properties prop = ofProperties.get(id);
            String path = id.getPath();

            // hardcoded special color stuff
            if (path.equals("stem") || path.equals("melon_stem") || path.equals("pumpkin_stem")) {
                Colormap colormap = Colormap.simple((state, level, pos, m) -> state != null && state.hasProperty(StemBlock.AGE) ? state.getValue(StemBlock.AGE) / 7f : 0,
                        IColormapNumberProvider.ZERO);

                List<Block> targets = new ArrayList<>();
                // so stem maps to both
                if (!path.contains("melon")) {
                    targets.add(Blocks.PUMPKIN_STEM);
                    targets.add(Blocks.ATTACHED_PUMPKIN_STEM);
                }
                if (!path.contains("pumpkin")) {
                    targets.add(Blocks.MELON_STEM);
                    targets.add(Blocks.ATTACHED_MELON_STEM);
                }
                map.put(id, BlockPropertyModifier.coloringBlocks(colormap, targets));
            } else if (path.equals("redstone_wire")) {
                Colormap colormap = Colormap.simple((state, level, pos, m) -> state != null ? state.getValue(RedStoneWireBlock.POWER) / 15f : 0,
                        IColormapNumberProvider.ZERO);

                map.put(id, BlockPropertyModifier.coloringBlocks(colormap, Blocks.REDSTONE_WIRE));
            } else if (prop != null) {
                try {
                    BlockPropertyModifier modifier = convertOFProperty(prop, id);
                    map.put(id, modifier);
                } catch (Exception e) {
                    Polytone.LOGGER.error("FAILED TO CONVERT OPTIFINE COLORMAP AT {}: ", id, e);
                }
            }
        }
        return map;

    }

    public static final Decoder<BlockPropertyModifier> OF_JSON_CODEC = RecordCodecBuilder.create(i -> i.group(
            StrOpt.of(Codec.STRING, "format", "").forGetter(c -> ""),
            StrOpt.of(Codec.STRING.listOf(), "blocks", List.of()).forGetter(c -> List.of()),
            StrOpt.of(ColorUtils.CODEC, "color").forGetter(c -> Optional.empty()),
            StrOpt.of(Codec.STRING.xmap(Integer::parseInt, String::valueOf), "yVariance").forGetter(c -> Optional.empty()),
            StrOpt.of(Codec.STRING.xmap(Integer::parseInt, String::valueOf), "yoffset").forGetter(c -> Optional.empty()),
            StrOpt.of(Codec.STRING, "source").forGetter(c -> Optional.empty())
    ).apply(i, LegacyHelper::decodeOFPropertyJson));

    private static BlockPropertyModifier decodeOFPropertyJson(String format, List<String> targets,
                                                              Optional<Integer> singleColor, Optional<Integer> yVariance,
                                                              Optional<Integer> yoffset, Optional<String> sourceTexture) {

        Set<ResourceLocation> set = null;
        Colormap colormap;
        if (!targets.isEmpty()) {
            set = targets.stream()
                    .filter(s -> {
                        // fuck this i wont parse numerical shit
                        try {
                            int iHateOptishit = Integer.parseInt(s);
                            // return BuiltInRegistries.BLOCK.getKey(BuiltInRegistries.BLOCK.byId(iHateOptishit));
                            return false;
                        } catch (Exception ignored) {
                        }
                        return true;
                    }).map(ResourceLocation::new)
                    .collect(Collectors.toSet());
            set.forEach(LegacyHelper::forceBlockToHaveTintIndex);

        }
        Integer col = singleColor.orElse(null);
        if ("fixed".equals(format)) {
            colormap = Colormap.fixed();
        } else if ("grid".equals(format)) {
            colormap = Colormap.biomeId();
            //variance and y offset are ignored. todo: add
        } else {
            colormap = Colormap.defTriangle();
        }
        if (col != null) {
            int[][] matrix = {{col}};
            colormap.acceptTexture(new ArrayImage(matrix));
        } else {
            if (sourceTexture.isPresent()) {

                // assumes id is minecraft. Not ideal.. too bad
                ResourceLocation id = new ResourceLocation("none");
                String source = sourceTexture.get().replace("~/colormap/", id.getNamespace() + ":");
                if (source.contains("./")) {
                    // resolve relative paths
                    String path = id.getPath();
                    int index = path.lastIndexOf('/');
                    String directoryPath = index == -1 ? "" : path.substring(0, index + 1);
                    source = source.replace("./", id.getNamespace() + ":" + directoryPath);
                }
                colormap.setExplicitTargetTexture(new ResourceLocation(source));
            }
        }
        return new BlockPropertyModifier(Optional.of(colormap),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.ofNullable(set), false);
    }


    public static BlockPropertyModifier convertOFProperty(Properties properties, ResourceLocation id) {
        Set<ResourceLocation> set = null;
        Colormap colormap;
        var targets = properties.getProperty("blocks");
        if (targets != null) {
            set = Arrays.stream(targets.split(" "))
                    .filter(s -> {
                        // fuck this i wont parse numerical shit
                        try {
                            int iHateOptishit = Integer.parseInt(s);
                            // return BuiltInRegistries.BLOCK.getKey(BuiltInRegistries.BLOCK.byId(iHateOptishit));
                            return false;
                        } catch (Exception ignored) {
                        }
                        return true;
                    }).map(ResourceLocation::new)
                    .collect(Collectors.toSet());
            set.forEach(LegacyHelper::forceBlockToHaveTintIndex);

        }
        String format = properties.getProperty("format");
        Integer col = null;
        String singleColor = properties.getProperty("color");
        if (singleColor != null) {
            col = Integer.parseInt(singleColor, 16);
        }
        if ("fixed".equals(format)) {
            colormap = Colormap.fixed();
        } else if ("grid".equals(format)) {
            colormap = Colormap.biomeId();
            //variance and y offset are ignored. todo: add
        } else {
            colormap = Colormap.defTriangle();
        }
        if (col != null) {
            int[][] matrix = {{col}};
            colormap.acceptTexture(new ArrayImage(matrix));
        } else {
            String source = properties.getProperty("source");
            if (source != null) {
                if (source.contains("~")) {
                    source = source.replace("~/colormap/", id.getNamespace() + ":");
                } else {
                    // resolve relative paths
                    String path = id.getPath();
                    int index = path.lastIndexOf('/');
                    String directoryPath = index == -1 ? "" : path.substring(0, index + 1);
                    source = (id.getNamespace() + ":" + directoryPath) + source.replace("./", "");
                }
                colormap.setExplicitTargetTexture(new ResourceLocation(source));
            }
        }
        return new BlockPropertyModifier(Optional.of(colormap),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.ofNullable(set), false);
    }

    public static Map<ResourceLocation, BlockPropertyModifier> convertInlinedPalettes(
            Map<ResourceLocation, String> inlineColormaps) {
        Map<ResourceLocation, BlockPropertyModifier> map = new HashMap<>();

        int k = 0;
        for (var special : inlineColormaps.entrySet()) {
            ResourceLocation texturePath = special.getKey();
            Colormap colormap = Colormap.defTriangle();
            colormap.setExplicitTargetTexture(texturePath);

            Set<ResourceLocation> blockTargets = new HashSet<>();
            for (var name : special.getValue().split(" ")) {
                if (name.isEmpty()) continue;
                ResourceLocation blockId = new ResourceLocation(name);
                blockTargets.add(blockId);
                forceBlockToHaveTintIndex(blockId);
            }
            if (!blockTargets.isEmpty()) {
                BlockPropertyModifier mod = BlockPropertyModifier.coloringBlocks(colormap, blockTargets);

                // unique id just because
                map.put(texturePath.withSuffix("-color_prop_palette_" + k++), mod);
            }
        }
        return map;
    }

    private static void forceBlockToHaveTintIndex(ResourceLocation blockId) {
        var b = BuiltInRegistries.BLOCK.getOptional(blockId);
        if (b.isPresent()) {
            Block block = b.get();
            if (block != Blocks.REDSTONE_WIRE && block != Blocks.PUMPKIN_STEM && block != Blocks.MELON_STEM) {
                Polytone.VARIANT_TEXTURES.addTintOverrideHack(block);
            }
        }
    }

    public static int getBiomeId(Biome biome, Registry<Biome> biomeRegistry) {
        ResourceLocation id = biomeRegistry.getKey(biome);
        return BIOME_ID_MAP.getOrDefault(id, 0);
    }

    //whateve optifine uses...
    private static final Object2IntMap<ResourceLocation> BIOME_ID_MAP = Util.make(() -> {
        Object2IntMap<ResourceLocation> map = new Object2IntOpenHashMap<>();
        // Add entries to the map
        map.put(new ResourceLocation("the_void"), 0);
        map.put(new ResourceLocation("plains"), 1);
        map.put(new ResourceLocation("sunflower_plains"), 2);
        map.put(new ResourceLocation("snowy_plains"), 3);
        map.put(new ResourceLocation("ice_spikes"), 4);
        map.put(new ResourceLocation("desert"), 5);
        map.put(new ResourceLocation("swamp"), 6);
        map.put(new ResourceLocation("mangrove_swamp"), 7);
        map.put(new ResourceLocation("forest"), 8);
        map.put(new ResourceLocation("flower_forest"), 9);
        map.put(new ResourceLocation("birch_forest"), 10);
        map.put(new ResourceLocation("dark_forest"), 11);
        map.put(new ResourceLocation("old_growth_birch_forest"), 12);
        map.put(new ResourceLocation("old_growth_pine_taiga"), 13);
        map.put(new ResourceLocation("old_growth_spruce_taiga"), 14);
        map.put(new ResourceLocation("taiga"), 15);
        map.put(new ResourceLocation("snowy_taiga"), 16);
        map.put(new ResourceLocation("savanna"), 17);
        map.put(new ResourceLocation("savanna_plateau"), 18);
        map.put(new ResourceLocation("windswept_hills"), 19);
        map.put(new ResourceLocation("windswept_gravelly_hills"), 20);
        map.put(new ResourceLocation("windswept_forest"), 21);
        map.put(new ResourceLocation("windswept_savanna"), 22);
        map.put(new ResourceLocation("jungle"), 23);
        map.put(new ResourceLocation("sparse_jungle"), 24);
        map.put(new ResourceLocation("bamboo_jungle"), 25);
        map.put(new ResourceLocation("badlands"), 26);
        map.put(new ResourceLocation("eroded_badlands"), 27);
        map.put(new ResourceLocation("wooded_badlands"), 28);
        map.put(new ResourceLocation("meadow"), 29);
        map.put(new ResourceLocation("cherry_grove"), 30);
        map.put(new ResourceLocation("grove"), 31);
        map.put(new ResourceLocation("snowy_slopes"), 32);
        map.put(new ResourceLocation("frozen_peaks"), 33);
        map.put(new ResourceLocation("jagged_peaks"), 34);
        map.put(new ResourceLocation("stony_peaks"), 35);
        map.put(new ResourceLocation("river"), 36);
        map.put(new ResourceLocation("frozen_river"), 37);
        map.put(new ResourceLocation("beach"), 38);
        map.put(new ResourceLocation("snowy_beach"), 39);
        map.put(new ResourceLocation("stony_shore"), 40);
        map.put(new ResourceLocation("warm_ocean"), 41);
        map.put(new ResourceLocation("lukewarm_ocean"), 42);
        map.put(new ResourceLocation("deep_lukewarm_ocean"), 43);
        map.put(new ResourceLocation("ocean"), 44);
        map.put(new ResourceLocation("deep_ocean"), 45);
        map.put(new ResourceLocation("cold_ocean"), 46);
        map.put(new ResourceLocation("deep_cold_ocean"), 47);
        map.put(new ResourceLocation("frozen_ocean"), 48);
        map.put(new ResourceLocation("deep_frozen_ocean"), 49);
        map.put(new ResourceLocation("mushroom_fields"), 50);
        map.put(new ResourceLocation("dripstone_caves"), 51);
        map.put(new ResourceLocation("lush_caves"), 52);
        map.put(new ResourceLocation("deep_dark"), 53);
        map.put(new ResourceLocation("nether_wastes"), 54);
        map.put(new ResourceLocation("warped_forest"), 55);
        map.put(new ResourceLocation("crimson_forest"), 56);
        map.put(new ResourceLocation("soul_sand_valley"), 57);
        map.put(new ResourceLocation("basalt_deltas"), 58);
        map.put(new ResourceLocation("the_end"), 59);
        map.put(new ResourceLocation("end_highlands"), 60);
        map.put(new ResourceLocation("end_midlands"), 61);
        map.put(new ResourceLocation("small_end_islands"), 62);
        map.put(new ResourceLocation("end_barrens"), 63);
        return map;
    });


    public static void convertOfBlockToFluidProp(Map<ResourceLocation, BlockPropertyModifier> parsedModifiers,
                                                 Map<ResourceLocation, ArrayImage> textures) {

        Map<ResourceLocation, BlockPropertyModifier> filtered = new HashMap<>();
        Map<ResourceLocation, ArrayImage> filteredTextures = new HashMap<>();
        for (var entry : parsedModifiers.entrySet()) {
            ResourceLocation id = entry.getKey();
            BlockPropertyModifier modifier = entry.getValue();
            if (id.getPath().contains("water") || id.getPath().contains("lava")) {
                filtered.put(id, modifier);
            }
        }
        for (var entry : textures.entrySet()) {
            ResourceLocation id = entry.getKey();
            ArrayImage modifier = entry.getValue();
            if (id.getPath().contains("water") || id.getPath().contains("lava")) {
                filteredTextures.put(id, modifier);
            }
        }
        textures.keySet().removeAll(filteredTextures.keySet());
        parsedModifiers.keySet().removeAll(filtered.keySet());

        Polytone.FLUID_PROPERTIES.addConvertedBlockProperties(filtered, filteredTextures);
    }


}
