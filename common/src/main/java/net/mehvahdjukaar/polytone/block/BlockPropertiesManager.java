package net.mehvahdjukaar.polytone.block;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class BlockPropertiesManager {

    private static final Map<Block, BlockPropertyModifier> VANILLA_PROPERTIES = new HashMap<>();

    private static final Map<ResourceLocation, BlockPropertyModifier> MODIFIERS = new HashMap<>();


    public static void process(Map<ResourceLocation, JsonElement> blockPropertiesJsons, Map<ResourceLocation, Map<Integer, ArrayImage>> texturesProperties, Set<ResourceLocation> usedTextures) {


        for (var j : blockPropertiesJsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            BlockPropertyModifier prop = BlockPropertyModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Client Block Property with json id {} - error: {}", id, errorMsg))
                    .getFirst();

            //fill inline colormaps textures
            var colormap = prop.tintGetter();
            if (colormap.isPresent() && colormap.get() instanceof Colormap c && !c.isReference()) {
                ColormapsManager.fillColormapPalette(texturesProperties, id, c, usedTextures);
            }

            MODIFIERS.put(id, prop);
        }

        // creates orphaned texture colormaps & properties
        texturesProperties.keySet().removeAll(usedTextures);

        for (var t : texturesProperties.entrySet()) {
            ResourceLocation id = t.getKey();
            Colormap defaultColormap = Colormap.createDefault(t.getValue().keySet());
            ColormapsManager.fillColormapPalette(texturesProperties, id, defaultColormap, usedTextures);

            BlockPropertyModifier defaultProp = new BlockPropertyModifier(Optional.of(defaultColormap),
                    Optional.empty(), Optional.empty(), Optional.empty());

            MODIFIERS.put(id, defaultProp);
        }
    }


    public static void apply() {
        for (var p : MODIFIERS.entrySet()) {
            var block = Polytone.getTarget(p.getKey(), BuiltInRegistries.BLOCK);
            if (block != null) {
                Block b = block.getFirst();
                BlockPropertyModifier value = p.getValue();
                VANILLA_PROPERTIES.put(b, value.apply(b));
            }
        }
        if (!VANILLA_PROPERTIES.isEmpty())
            Polytone.LOGGER.info("Applied {} Custom Block Properties", VANILLA_PROPERTIES.size());

        MODIFIERS.clear();
    }


    public static void reset() {
        for (var e : VANILLA_PROPERTIES.entrySet()) {
            e.getValue().apply(e.getKey());
        }
        VANILLA_PROPERTIES.clear();
    }
}
