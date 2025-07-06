package net.mehvahdjukaar.polytone.utils;

import com.google.common.collect.LinkedListMultimap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.block.BlockPropertyModifier;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.colormap.IColormapNumberProvider;
import net.mehvahdjukaar.polytone.dimension.DimensionEffectsModifier;
import net.mehvahdjukaar.polytone.dimension.DimensionTarget;
import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.minecraft.Util;
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
import org.jetbrains.annotations.Nullable;

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


    public static Map<ResourceLocation, Parsed<BlockPropertyModifier>> convertBlockProperties(
            Map<ResourceLocation, Properties> ofProperties, Map<ResourceLocation, ArrayImage> textures) {

        List<ResourceLocation> ids = new ArrayList<>();
        ids.addAll(ofProperties.keySet());
        ids.addAll(textures.keySet());

        Map<ResourceLocation, Parsed<BlockPropertyModifier>> map = new HashMap<>();

        for (ResourceLocation id : ids) {
            @Nullable Properties prop = ofProperties.get(id);
            String path = id.getPath();

            // hardcoded special color stuff
            if (path.equals("stem") || path.equals("melon_stem") || path.equals("pumpkin_stem")) {
                Colormap colormap = Colormap.simple((state, level, pos, m, i) -> state != null && state.hasProperty(StemBlock.AGE) ? state.getValue(StemBlock.AGE) / 7f : 0,
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
                map.put(id, withCond(id, prop, BlockPropertyModifier.coloringBlocks(colormap, targets)));
            } else if (path.equals("redstone_wire")) {
                Colormap colormap = Colormap.simple(new IColormapNumberProvider() {
                    @Override
                    public float getValue(BlockState state, BlockPos pos, Biome biome, BiomeIdMapper mapper, ItemStack stack) {
                        return state != null ? (1 - (state.getValue(RedStoneWireBlock.POWER) / 15f)) : 1;
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

                map.put(id, withCond(id, prop, BlockPropertyModifier.coloringBlocks(colormap, Blocks.REDSTONE_WIRE)));
            } else if (prop != null) {
                try {
                    BlockPropertyModifier modifier = convertOFProperty(prop, id);
                    map.put(id, withCond(id, prop, modifier));
                } catch (Exception e) {
                    Polytone.LOGGER.error("FAILED TO CONVERT OPTIFINE COLORMAP AT {}. Its likely the file has errors: ", id, e);
                }
            }
        }
        return map;

    }

    private static <T> Parsed<T> withCond(ResourceLocation id, @Nullable Properties prop, T t) {
        return Parsed.of(t, id, prop == null || checkConditions(prop));
    }

    private static boolean checkConditions(Properties prop) {
        boolean ignored = prop.getOrDefault("polytone_ignore", false).equals("true");
        if (ignored) {
            return false;
        }
        List<String> requireMods = List.of(prop.getProperty("require_mods", "").split(" "));
        for (String s : requireMods) {
            if (!PlatStuff.isModLoaded(s)) {
                return false;
            }
        }
        return true;
    }

    public static final Decoder<BlockPropertyModifier> OF_JSON_CODEC = RecordCodecBuilder.create(i -> i.group(
            StrOpt.of(Codec.STRING, "format", "").forGetter(c -> ""),
            StrOpt.of(Codec.STRING.listOf(), "blocks", List.of()).forGetter(c -> List.of()),
            StrOpt.of(ColorUtils.CODEC, "color").forGetter(c -> Optional.empty()),
            StrOpt.of(Codec.STRING.xmap(Integer::parseInt, String::valueOf), "yVariance").forGetter(c -> Optional.empty()),
            StrOpt.of(Codec.STRING.xmap(Integer::parseInt, String::valueOf), "yoffset").forGetter(c -> Optional.empty()),
            StrOpt.of(Codec.STRING, "source").forGetter(c -> Optional.empty()),
            StrOpt.of(Codec.BOOL,"force_tint", true).forGetter(c -> true)
    ).apply(i, LegacyHelper::decodeOFPropertyJson));

    private static BlockPropertyModifier decodeOFPropertyJson(String format, List<String> targets,
                                                              Optional<Integer> singleColor, Optional<Integer> yVariance,
                                                              Optional<Integer> yoffset, Optional<String> sourceTexture,
                                                              boolean forceTint) {

        Set<ResourceLocation> set = new HashSet<>();
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
                Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(),
                Optional.empty(),
                Optional.empty(), List.of(), List.of(),
                Optional.empty(), Optional.empty(),
                false, Targets.ofIds(set), false);
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
                    }).map(ResourceLocation::new)
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
                colormap.setExplicitTargetTexture(new ResourceLocation(source));
            }
        }
        return new BlockPropertyModifier(Optional.of(colormap),
                Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(),
                Optional.empty(),
                Optional.empty(), List.of(), List.of(), Optional.empty(),
                Optional.empty(), false, Targets.ofOptionalIds(set), false);
    }

    public static Map<ResourceLocation, Parsed<BlockPropertyModifier>> convertInlinedPalettes(
            Map<ResourceLocation, String> inlineColormaps) {
        Map<ResourceLocation, Parsed<BlockPropertyModifier>> map = new HashMap<>();

        int k = 0;
        for (var special : inlineColormaps.entrySet()) {
            ResourceLocation texturePath = special.getKey();
            Colormap colormap = Colormap.createDefTriangle();
            colormap.setExplicitTargetTexture(texturePath);

            Set<ResourceLocation> blockTargets = new HashSet<>();
            for (var name : special.getValue().split(" ")) {
                if (name.isEmpty()) continue;
                ResourceLocation blockId = ResourceLocation.tryParse(name);
                blockTargets.add(blockId);
                forceBlockToHaveTintIndex(blockId);
            }
            if (!blockTargets.isEmpty()) {
                BlockPropertyModifier mod = BlockPropertyModifier.coloringBlocks(colormap, blockTargets);

                // unique id just because
                ResourceLocation id = texturePath.withSuffix("-color_prop_palette_" + k++);
                map.put(id, Parsed.success(mod, id));
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
        map.put(biomeResKey("pale_garden"), 64);
        return map;
    });

    private static ResourceKey<Biome> biomeResKey(String endBarrens) {
        return ResourceKey.create(Registries.BIOME, new ResourceLocation(endBarrens));
    }


    public static void convertOfBlockToFluidProp(LinkedListMultimap<ResourceLocation, Parsed<BlockPropertyModifier>> parsedModifiers,
                                                 Map<ResourceLocation, ArrayImage> textures) {

        Map<ResourceLocation, Parsed<BlockPropertyModifier>> fluid = new HashMap<>();
        Map<ResourceLocation, Parsed<BlockPropertyModifier>> fog = new HashMap<>();
        Map<ResourceLocation, ArrayImage> filteredTextures = new HashMap<>();
        for (var entry : parsedModifiers.entries()) {
            ResourceLocation id = entry.getKey();
            Parsed<BlockPropertyModifier> parsed = entry.getValue();
            BlockPropertyModifier modifier = parsed.getResultOrPartial();
            var colormap = modifier.getColormap();
            if (colormap instanceof Colormap c) {
                //  if (!id.getNamespace().equals("minecraft")) continue;
                String path = id.getPath();
                if (path.contains("water") || path.contains("lava")) {

                    if (path.endsWith("_fog") || path.contains("under")) fog.put(id, parsed);
                    else fluid.put(id, parsed);

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

        Map<ResourceLocation, Parsed<FluidPropertyModifier>> converted = new HashMap<>();

        for (var f : fluid.entrySet()) {
            // ignore targets as those are block targets anyways
            var parsed = f.getValue();
            var mod = parsed.getResultOrPartial();
            ResourceLocation id = f.getKey();
            Targets targets = mod.targets();
            targets.addSimple(id);
            targets.addSimple(id.withPrefix("flowing_"));
            var fogMod = Optional.ofNullable(fog.get(id.withSuffix("_fog")))
                    .map(Parsed::getResultOrPartial);
            FluidPropertyModifier modifier = new FluidPropertyModifier(mod.tintGetter(),
                    fogMod.map(BlockPropertyModifier::getColormap),
                    Optional.empty(), Optional.empty(),
                    targets);
            var parsedModifier = Parsed.of(modifier, id, parsed.isEnabled());
            converted.put(id, parsedModifier);
        }

        Polytone.FLUID_MODIFIERS.addConvertedBlockProperties(converted, filteredTextures);
    }

    public static void convertOfBlockToDimensionProperties(LinkedListMultimap<ResourceLocation, Parsed<BlockPropertyModifier>> parsedModifiers,
                                                           Map<ResourceLocation, ArrayImage> textures) {
        Map<ResourceLocation, Parsed<BlockPropertyModifier>> filtered = new HashMap<>();
        Map<ResourceLocation, ArrayImage> filteredTextures = new HashMap<>();
        Pattern fogP = Pattern.compile("minecraft:fog[0-2]");
        Pattern skyP = Pattern.compile("minecraft:sky[0-2]");
        for (var entry : parsedModifiers.entries()) {
            ResourceLocation id = entry.getKey();
            String stringId = id.toString();
            var modifier = entry.getValue();
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
    private static void addConvertedBlockProperties(Map<ResourceLocation, Parsed<BlockPropertyModifier>> modifiers, Map<ResourceLocation, ArrayImage> textures) {
        String[] names = new String[]{"overworld", "the_nether", "the_end"};
        Map<ResourceLocation, Parsed<DimensionEffectsModifier>> converted = new HashMap<>();
        for (int i = 0; i <= 2; i++) {
            IColorGetter skyCol;
            IColorGetter fogCol;
            boolean skyEnabled = true;
            boolean fogEnabled = true;
            {
                ResourceLocation skyKey = ResourceLocation.tryParse("sky" + i);
                var skyMod = modifiers.get(skyKey);
                ArrayImage skyImage = textures.get(skyKey);

                skyCol = skyMod != null ? skyMod.getResultOrPartial().getColormap() : (skyImage == null ? null : Colormap.createDefTriangle());
                if (skyCol != null) {
                    ColormapsManager.tryAcceptingTexture(textures, skyKey, skyCol, new HashSet<>(), true);
                }
                skyEnabled = skyMod == null || skyMod.isEnabled();
            }
            {
                ResourceLocation fogKey = new ResourceLocation("fog" + i);
                var fogMod = modifiers.get(fogKey);
                ArrayImage fogImage = textures.get(fogKey);

                fogCol = fogMod != null ? fogMod.getResultOrPartial().getColormap() : (fogImage == null ? null : Colormap.createDefTriangle());
                if (fogCol != null) {
                    ColormapsManager.tryAcceptingTexture(textures, fogKey, fogCol, new HashSet<>(), true);
                }
                fogEnabled = fogMod == null || fogMod.isEnabled();
            }
            if (fogCol != null || skyCol != null) {
                var mod = new DimensionEffectsModifier(Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.ofNullable(fogCol), Optional.ofNullable(skyCol), Optional.empty(),
                        false,false, Optional.empty(), DimensionTarget.EMPTY);

                ResourceLocation id = new ResourceLocation(names[i]);
                boolean enabled = fogEnabled || skyEnabled;
                var parsedMod = Parsed.of(mod, id, enabled);
                converted.put(id, parsedMod);
            }
        }
        Polytone.DIMENSION_MODIFIERS.addConvertedBlockProperties(converted);
    }
}
