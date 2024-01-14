package net.mehvahdjukaar.polytone.fluid;

import com.mojang.serialization.JsonOps;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FluidPropertiesManager extends JsonImgPartialReloader {

    private final Map<Fluid, FluidPropertyModifier> fluidColormaps = new HashMap<>();

    public FluidPropertiesManager() {
        super("fluid_properties");
    }

    @Override
    public void process(Resources resources) {
        var jsons = resources.jsons();
        var textures = ArrayImage.groupTextures(resources.textures());

        Set<ResourceLocation> usedTextures = new HashSet<>();

        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            FluidPropertyModifier modifier = FluidPropertyModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Fluid Color Modifier with json id {} - error: {}", id, errorMsg))
                    .getFirst();

            //fill inline colormaps colormapTextures
            BlockColor colormap = modifier.colormap().orElse(null);
            if (colormap instanceof Colormap c && !c.isReference()) {
                ColormapsManager.fillColormapPalette(textures, id, c, usedTextures);
            }
            tryAdd(id, modifier);
        }

        // creates orphaned texture colormaps & properties
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            ResourceLocation id = t.getKey();
            Colormap defaultColormap = Colormap.createDefault(t.getValue().keySet());
            ColormapsManager.fillColormapPalette(textures, id, defaultColormap, usedTextures);

            tryAdd(id, new FluidPropertyModifier(Optional.of(defaultColormap)));
        }
    }

    @Override
    protected void reset() {
        fluidColormaps.clear();
        clearSpecial();
    }

    private void tryAdd(ResourceLocation id, FluidPropertyModifier colormap) {
        var fluid = Polytone.getTarget(id, BuiltInRegistries.FLUID);
        if (fluid != null) {
            fluidColormaps.put(fluid.getFirst(), colormap);
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


    public int modifyColor(int original, @Nullable BlockAndTintGetter level,
                           @Nullable BlockPos pos, @Nullable BlockState state,
                           FluidState fluidState) {
        var modifier = fluidColormaps.get(fluidState.getType());
        if (modifier != null) {
            var col = modifier.getColormap();
            if (col != null) {
                return col.getColor(state, level, pos, -1) | 0xff000000;
            }
        }
        return original;
    }
}
