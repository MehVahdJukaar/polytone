package net.mehvahdjukaar.polytone.fluid;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FluidPropertiesManager {

    private static final Map<Fluid, FluidPropertyModifier> FLUID_COLORMAPS = new HashMap<>();

    public static void process(Map<ResourceLocation, JsonElement> elements, Map<ResourceLocation, Map<Integer, ArrayImage>> texturesProperties, Set<ResourceLocation> usedTextures) {
        FLUID_COLORMAPS.clear();
        clearSpecial();
        
        for (var j : elements.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            FluidPropertyModifier modifier = FluidPropertyModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Fluid Color Modifier with json id {} - error: {}", id, errorMsg))
                    .getFirst();

            //fill inline colormaps colormapTextures
            BlockColor colormap = modifier.colormap().orElse(null);
            if (colormap instanceof Colormap c && !c.isReference()) {
                ColormapsManager.fillColormapPalette(texturesProperties, id, c, usedTextures);
            }
            tryAdd(id, modifier);
        }

        // creates orphaned texture colormaps & properties
        texturesProperties.keySet().removeAll(usedTextures);

        for (var t : texturesProperties.entrySet()) {
            ResourceLocation id = t.getKey();
            Colormap defaultColormap = Colormap.createDefault(t.getValue().keySet());
            ColormapsManager.fillColormapPalette(texturesProperties, id, defaultColormap, usedTextures);

            tryAdd(id, new FluidPropertyModifier(Optional.of(defaultColormap)));
        }
    }


    private static void tryAdd(ResourceLocation id, FluidPropertyModifier colormap) {
        var fluid = Polytone.getTarget(id, BuiltInRegistries.FLUID);
        if (fluid != null) {
            FLUID_COLORMAPS.put(fluid.getFirst(), colormap);
        }
        tryAddSpecial(id, colormap);
    }

    @ExpectPlatform
    private static void tryAddSpecial(ResourceLocation id, FluidPropertyModifier colormap) {
        throw new AssertionError();
    }
    @ExpectPlatform
    private static void clearSpecial() {
        throw new AssertionError();
    }


    public static int modifyColor(int original, @Nullable BlockAndTintGetter level,
                                  @Nullable BlockPos pos , @Nullable BlockState state,
                                  FluidState fluidState) {
        var modifier = FLUID_COLORMAPS.get(fluidState.getType());
        if (modifier != null) {
            var col = modifier.getColormap();
            if (col!= null){
                return col.getColor(state, level, pos, -1) | 0xff000000;
            }
        }
        return original;
    }


}
