package net.mehvahdjukaar.polytone.utils;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.block.BlockPropertyModifier;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.IColormapNumberProvider;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
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
        for (var entry : map.entrySet()) {
            ResourceLocation id = entry.getKey();
            String path = id.getPath();
            String newPath = PATHS.get(path);
            if (newPath != null) {
                toUpdate.put(new ResourceLocation(id.getNamespace(), newPath), entry.getValue());
            }
        }
        map.putAll(toUpdate);
        return map;
    }

    public static Map<ResourceLocation, BlockPropertyModifier> convertBlockProperties(Map<ResourceLocation, Properties> ofProperties) {
        Map<ResourceLocation, BlockPropertyModifier> map = new HashMap<>();

        for (var entry : ofProperties.entrySet()) {
            ResourceLocation id = entry.getKey();
            Properties prop = entry.getValue();
            String path = id.getPath();

            // hardcoded special color stuff
            if (path.equals("stem") || path.equals("melon_stem") || path.equals("pumpkin_stem")) {
                Colormap colormap = Colormap.simple((state, level, pos) -> state != null && state.hasProperty(StemBlock.AGE) ? state.getValue(StemBlock.AGE) / 7f : 0,
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
                Colormap colormap = Colormap.simple((state, level, pos) -> state != null ? state.getValue(RedStoneWireBlock.POWER) / 15f : 0,
                        IColormapNumberProvider.ZERO);

                map.put(id, BlockPropertyModifier.coloringBlocks(colormap, Blocks.REDSTONE_WIRE));
            } else {
                BlockPropertyModifier modifier = convertOFProperty(prop, id);
                map.put(id, modifier);
            }

        }
        return map;

    }

    public static BlockPropertyModifier convertOFProperty(Properties properties, ResourceLocation id) {
        Set<ResourceLocation> set = null;
        Colormap colormap;
        var targets = properties.getProperty("blocks");
        if (targets != null) {
            set = Arrays.stream(targets.split(" "))
                    .map(ResourceLocation::new)
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
            source = source.replace("~/colormap/", id.getNamespace() + ":");
            colormap.setTargetTexture(new ResourceLocation(source));
        }
        return new BlockPropertyModifier(Optional.of(colormap),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.ofNullable(set));
    }

    public static Map<ResourceLocation, BlockPropertyModifier> convertInlinedPalettes(
            Map<ResourceLocation, String> inlineColormaps) {
        Map<ResourceLocation, BlockPropertyModifier> map = new HashMap<>();

        int k = 0;
        for (var special : inlineColormaps.entrySet()) {
            ResourceLocation texturePath = special.getKey();
            Colormap colormap = Colormap.defTriangle();
            colormap.setTargetTexture(texturePath);

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
                map.put(new ResourceLocation(texturePath.getNamespace(), texturePath.getPath()+"-color_prop_palette_" + k++), mod);
            }
        }
        return map;
    }

    private static void forceBlockToHaveTintIndex(ResourceLocation blockId) {
        var b = Registry.BLOCK.getOptional(blockId);
        b.ifPresent(Polytone.VARIANT_TEXTURES::addTintOverrideHack);
    }
}
