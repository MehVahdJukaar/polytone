package net.mehvahdjukaar.polytone.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.biome.BiomeIdMapper;
import net.mehvahdjukaar.polytone.block.BlockPropertyModifier;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.colormap.IColormapNumberProvider;
import net.mehvahdjukaar.polytone.dimension.DimensionEffectsModifier;
import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.regex.Pattern;
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
            ResourceLocation newPath = convertPath(id);
            if (!newPath.equals(id)) {
                toUpdate.put(newPath, entry.getValue());
                toRemove.add(id);
            }
        }
        toRemove.forEach(map.keySet()::remove);
        map.putAll(toUpdate);
        return map;
    }

    public static ResourceLocation convertPath(ResourceLocation id) {
        String path = PATHS.get(id.getPath());
        return path == null ? id : id.withPath(path);
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
                Colormap colormap = Colormap.simple(new IColormapNumberProvider() {
                    @Override
                    public float getValue(BlockState state, BlockPos pos, Biome biome, BiomeIdMapper mapper, ItemStack stack) {
                        return state != null && state.hasProperty(StemBlock.AGE) ? state.getValue(StemBlock.AGE) / 7f : 0;
                    }

                    @Override
                    public boolean usesBiome() {
                        return false;
                    }

                    @Override
                    public boolean usesPos() {
                        return false;
                    }
                }, IColormapNumberProvider.ZERO);

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
                Colormap colormap = Colormap.simple(new IColormapNumberProvider() {
                    @Override
                    public float getValue(BlockState state, BlockPos pos, Biome biome, BiomeIdMapper mapper, ItemStack stack) {
                        return state != null ? (1 - state.getValue(RedStoneWireBlock.POWER) / 15f) : 1;
                    }

                    @Override
                    public boolean usesBiome() {
                        return false;
                    }

                    @Override
                    public boolean usesPos() {
                        return false;
                    }
                }, IColormapNumberProvider.ZERO);

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
            Codec.STRING.optionalFieldOf("format", "").forGetter(c -> ""),
            Codec.STRING.listOf().optionalFieldOf("blocks", List.of()).forGetter(c -> List.of()),
            ColorUtils.CODEC.optionalFieldOf("color").forGetter(c -> Optional.empty()),
            Codec.STRING.xmap(Integer::parseInt, String::valueOf).optionalFieldOf("yVariance").forGetter(c -> Optional.empty()),
            Codec.STRING.xmap(Integer::parseInt, String::valueOf).optionalFieldOf("yoffset").forGetter(c -> Optional.empty()),
            Codec.STRING.optionalFieldOf("source").forGetter(c -> Optional.empty()),
            Codec.BOOL.optionalFieldOf("force_tint", true).forGetter(c -> true)
    ).apply(i, LegacyHelper::decodeOFPropertyJson));

    private static BlockPropertyModifier decodeOFPropertyJson(String format, List<String> targets,
                                                              Optional<Integer> singleColor, Optional<Integer> yVariance,
                                                              Optional<Integer> yoffset, Optional<String> sourceTexture,
                                                              boolean forceTint) {

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
                    }).map(ResourceLocation::tryParse)
                    .collect(Collectors.toSet());
            if (forceTint) set.forEach(LegacyHelper::forceBlockToHaveTintIndex);

        }
        Integer col = singleColor.orElse(null);
        if ("fixed".equals(format)) {
            colormap = Colormap.createFixed();
        } else if ("grid".equals(format)) {
            colormap = Colormap.createBiomeId();
            //variance and y offset are ignored. todo: add
        } else {
            colormap = Colormap.createDefTriangle();
        }
        if (col != null) {
            int[][] matrix = {{col}};
            colormap.acceptTexture(new ArrayImage(matrix));
        } else {
            if (sourceTexture.isPresent()) {

                // assumes id is minecraft. Not ideal.. too bad
                ResourceLocation id = ResourceLocation.parse("none");
                String source = sourceTexture.get().replace("~/colormap/", id.getNamespace() + ":");
                if (source.contains("./")) {
                    // resolve relative paths
                    String path = id.getPath();
                    int index = path.lastIndexOf('/');
                    String directoryPath = index == -1 ? "" : path.substring(0, index + 1);
                    source = source.replace("./", id.getNamespace() + ":" + directoryPath);
                }
                colormap.setExplicitTargetTexture(ResourceLocation.parse(source));
            }
        }
        return new BlockPropertyModifier(Optional.of(colormap),
                Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(),
                Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Targets.ofIds(set), false);
    }


    public static BlockPropertyModifier convertOFProperty(Properties properties, ResourceLocation id) {
        Set<ResourceLocation> set;
        Colormap colormap;
        boolean forceTint = Boolean.parseBoolean(properties.getProperty("force_tint", "true"));
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
                    }).map(ResourceLocation::tryParse)
                    .collect(Collectors.toSet());
            if (forceTint) set.forEach(LegacyHelper::forceBlockToHaveTintIndex);
        } else set = Set.of();

        String format = properties.getProperty("format");
        Integer col = null;
        String singleColor = properties.getProperty("color");
        if (singleColor != null) {
            col = Integer.parseInt(singleColor, 16);
        }
        if ("fixed".equals(format)) {
            colormap = Colormap.createFixed();
        } else if ("grid".equals(format)) {
            colormap = Colormap.createBiomeId();
            //variance and y offset are ignored. todo: add
        } else {
            colormap = Colormap.createDefTriangle();
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
                colormap.setExplicitTargetTexture(ResourceLocation.parse(source));
            }
        }
        return new BlockPropertyModifier(Optional.of(colormap),
                Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(),
                Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Targets.ofIds(set), false);
    }

    public static Map<ResourceLocation, BlockPropertyModifier> convertInlinedPalettes(
            Map<ResourceLocation, String> inlineColormaps) {
        Map<ResourceLocation, BlockPropertyModifier> map = new HashMap<>();

        int k = 0;
        for (var special : inlineColormaps.entrySet()) {
            ResourceLocation texturePath = special.getKey();
            Colormap colormap = Colormap.createDefTriangle();
            colormap.setExplicitTargetTexture(texturePath);

            Set<ResourceLocation> blockTargets = new HashSet<>();
            for (var name : special.getValue().split(" ")) {
                if (name.isEmpty()) continue;
                ResourceLocation blockId = ResourceLocation.parse(name);
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

    public static int getBiomeId(Biome biome) {
        return BIOME_ID_MAP.getOrDefault(BiomeKeysCache.get(biome), 0);
    }

    //whateve optifine uses...
    private static final Object2IntMap<ResourceKey<Biome>> BIOME_ID_MAP = Util.make(() -> {
        Object2IntMap<ResourceKey<Biome>> map = new Object2IntOpenHashMap<>();
        // Add entries to the map
        map.put(biomeResKey("the_void"), 0);
        map.put(biomeResKey("plains"), 1);
        map.put(biomeResKey("sunflower_plains"), 2);
        map.put(biomeResKey("snowy_plains"), 3);
        map.put(biomeResKey("ice_spikes"), 4);
        map.put(biomeResKey("desert"), 5);
        map.put(biomeResKey("swamp"), 6);
        map.put(biomeResKey("mangrove_swamp"), 7);
        map.put(biomeResKey("forest"), 8);
        map.put(biomeResKey("flower_forest"), 9);
        map.put(biomeResKey("birch_forest"), 10);
        map.put(biomeResKey("dark_forest"), 11);
        map.put(biomeResKey("old_growth_birch_forest"), 12);
        map.put(biomeResKey("old_growth_pine_taiga"), 13);
        map.put(biomeResKey("old_growth_spruce_taiga"), 14);
        map.put(biomeResKey("taiga"), 15);
        map.put(biomeResKey("snowy_taiga"), 16);
        map.put(biomeResKey("savanna"), 17);
        map.put(biomeResKey("savanna_plateau"), 18);
        map.put(biomeResKey("windswept_hills"), 19);
        map.put(biomeResKey("windswept_gravelly_hills"), 20);
        map.put(biomeResKey("windswept_forest"), 21);
        map.put(biomeResKey("windswept_savanna"), 22);
        map.put(biomeResKey("jungle"), 23);
        map.put(biomeResKey("sparse_jungle"), 24);
        map.put(biomeResKey("bamboo_jungle"), 25);
        map.put(biomeResKey("badlands"), 26);
        map.put(biomeResKey("eroded_badlands"), 27);
        map.put(biomeResKey("wooded_badlands"), 28);
        map.put(biomeResKey("meadow"), 29);
        map.put(biomeResKey("cherry_grove"), 30);
        map.put(biomeResKey("grove"), 31);
        map.put(biomeResKey("snowy_slopes"), 32);
        map.put(biomeResKey("frozen_peaks"), 33);
        map.put(biomeResKey("jagged_peaks"), 34);
        map.put(biomeResKey("stony_peaks"), 35);
        map.put(biomeResKey("river"), 36);
        map.put(biomeResKey("frozen_river"), 37);
        map.put(biomeResKey("beach"), 38);
        map.put(biomeResKey("snowy_beach"), 39);
        map.put(biomeResKey("stony_shore"), 40);
        map.put(biomeResKey("warm_ocean"), 41);
        map.put(biomeResKey("lukewarm_ocean"), 42);
        map.put(biomeResKey("deep_lukewarm_ocean"), 43);
        map.put(biomeResKey("ocean"), 44);
        map.put(biomeResKey("deep_ocean"), 45);
        map.put(biomeResKey("cold_ocean"), 46);
        map.put(biomeResKey("deep_cold_ocean"), 47);
        map.put(biomeResKey("frozen_ocean"), 48);
        map.put(biomeResKey("deep_frozen_ocean"), 49);
        map.put(biomeResKey("mushroom_fields"), 50);
        map.put(biomeResKey("dripstone_caves"), 51);
        map.put(biomeResKey("lush_caves"), 52);
        map.put(biomeResKey("deep_dark"), 53);
        map.put(biomeResKey("nether_wastes"), 54);
        map.put(biomeResKey("warped_forest"), 55);
        map.put(biomeResKey("crimson_forest"), 56);
        map.put(biomeResKey("soul_sand_valley"), 57);
        map.put(biomeResKey("basalt_deltas"), 58);
        map.put(biomeResKey("the_end"), 59);
        map.put(biomeResKey("end_highlands"), 60);
        map.put(biomeResKey("end_midlands"), 61);
        map.put(biomeResKey("small_end_islands"), 62);
        map.put(biomeResKey("end_barrens"), 63);
        return map;
    });

    private static ResourceKey<Biome> biomeResKey(String endBarrens) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.parse(endBarrens));
    }


    public static void convertOfBlockToFluidProp(Map<ResourceLocation, BlockPropertyModifier> parsedModifiers,
                                                 Map<ResourceLocation, ArrayImage> textures) {

        Map<ResourceLocation, BlockPropertyModifier> fluid = new HashMap<>();
        Map<ResourceLocation, BlockPropertyModifier> fog = new HashMap<>();
        Map<ResourceLocation, FluidPropertyModifier> converted = new HashMap<>();
        Map<ResourceLocation, ArrayImage> filteredTextures = new HashMap<>();
        for (var entry : parsedModifiers.entrySet()) {
            ResourceLocation id = entry.getKey();
            BlockPropertyModifier modifier = entry.getValue();
            var colormap = modifier.getColormap();
            if (colormap instanceof Colormap c) {
                if (!id.getNamespace().equals("minecraft")) continue;
                String path = id.getPath();
                if (path.contains("water") || path.contains("lava")) {

                    if (path.endsWith("_fog")) fog.put(id, modifier);
                    else fluid.put(id, modifier);

                    ResourceLocation targetTexture = c.getTargetTexture(id);
                    //uglyyy
                    c.setExplicitTargetTexture(LegacyHelper.convertPath(targetTexture));
                    if (textures.containsKey(targetTexture)) {
                        filteredTextures.put(targetTexture, textures.get(targetTexture));
                    }
                }
            }
        }
        for (var v : textures.entrySet()) {
            ResourceLocation id = v.getKey();
            if (id.getNamespace().equals("minecraft") && (id.getPath().contains("water") || id.getPath().contains("lava"))) {
                filteredTextures.put(id, v.getValue());
            }
        }

        textures.keySet().removeAll(filteredTextures.keySet());
        parsedModifiers.keySet().removeAll(fluid.keySet());

        for (var f : fluid.entrySet()) {
            // ignore targets as those are block targets anyways
            BlockPropertyModifier mod = f.getValue();
            ResourceLocation id = f.getKey();
            Targets targets = mod.targets();
            targets.addSimple(id);
            targets.addSimple(id.withSuffix("_flowing"));
            FluidPropertyModifier modifier = new FluidPropertyModifier(mod.tintGetter(),
                    Optional.ofNullable(fog.get(id.withSuffix("_fog")))
                            .map(BlockPropertyModifier::getColormap),
                    targets);
            converted.put(id, modifier);

        }

        Polytone.FLUID_MODIFIERS.addConvertedBlockProperties(converted, filteredTextures);
    }

    public static void convertOfBlockToDimensionProperties(Map<ResourceLocation, BlockPropertyModifier> parsedModifiers,
                                                           Map<ResourceLocation, ArrayImage> textures) {
        Map<ResourceLocation, BlockPropertyModifier> filtered = new HashMap<>();
        Map<ResourceLocation, ArrayImage> filteredTextures = new HashMap<>();
        Pattern fogP = Pattern.compile("minecraft:fog[0-2]");
        Pattern skyP = Pattern.compile("minecraft:sky[0-2]");
        for (var entry : parsedModifiers.entrySet()) {
            ResourceLocation id = entry.getKey();
            String stringId = id.toString();
            BlockPropertyModifier modifier = entry.getValue();
            if (fogP.matcher(stringId).matches() || skyP.matcher(stringId).matches()) {
                filtered.put(id, modifier);
            }
        }
        for (var entry : textures.entrySet()) {
            ResourceLocation id = entry.getKey();
            String stringId = id.toString();
            ArrayImage modifier = entry.getValue();
            if (fogP.matcher(stringId).matches() || skyP.matcher(stringId).matches()) {
                filteredTextures.put(id, modifier);
            }
        }
        textures.keySet().removeAll(filteredTextures.keySet());
        parsedModifiers.keySet().removeAll(filtered.keySet());

        addConvertedBlockProperties(filtered, filteredTextures);

    }

    // fot OF fog and sky. shit code...
    private static void addConvertedBlockProperties(Map<ResourceLocation, BlockPropertyModifier> modifiers, Map<ResourceLocation, ArrayImage> textures) {
        String[] names = new String[]{"overworld", "the_nether", "the_end"};
        Map<ResourceLocation, DimensionEffectsModifier> converted = new HashMap<>();
        for (int i = 0; i <= 2; i++) {
            IColorGetter skyCol;
            IColorGetter fogCol;
            {
                ResourceLocation skyKey = ResourceLocation.parse("sky" + i);
                BlockPropertyModifier skyMod = modifiers.get(skyKey);
                ArrayImage skyImage = textures.get(skyKey);

                skyCol = skyMod != null ? skyMod.getColormap() : (skyImage == null ? null : Colormap.createDefTriangle());
                if (skyCol != null) {
                    ColormapsManager.tryAcceptingTexture(textures, skyKey, skyCol, new HashSet<>(), true);
                }
            }
            {
                ResourceLocation fogKey = ResourceLocation.parse("fog" + i);
                BlockPropertyModifier fogMod = modifiers.get(fogKey);
                ArrayImage fogImage = textures.get(fogKey);

                fogCol = fogMod != null ? fogMod.getColormap() : (fogImage == null ? null : Colormap.createDefTriangle());
                if (fogCol != null) {
                    ColormapsManager.tryAcceptingTexture(textures, fogKey, fogCol, new HashSet<>(), true);
                }
            }
            if (fogCol != null || skyCol != null) {
                var mod = new DimensionEffectsModifier(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.ofNullable(fogCol), Optional.ofNullable(skyCol), Optional.empty(),
                        false, false, Optional.empty(), Targets.EMPTY);

                converted.put(ResourceLocation.parse(names[i]), mod);
            }
        }
        Polytone.DIMENSION_MODIFIERS.addConvertedBlockProperties(converted);
    }
}
