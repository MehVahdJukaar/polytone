package net.mehvahdjukaar.polytone.fluid;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.mehvahdjukaar.polytone.utils.LegacyHelper;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FluidPropertiesManager extends JsonImgPartialReloader {

    private final Map<Fluid, FluidPropertyModifier> modifiers = new HashMap<>();

    public FluidPropertiesManager() {
        super("fluid_properties");
    }

    @Override
    protected Resources prepare(ResourceManager resourceManager) {
        Map<ResourceLocation, JsonElement> jsons = new HashMap<>();
        scanDirectory(resourceManager, path(), GSON, jsons);

        Map<ResourceLocation, ArrayImage> textures = new HashMap<>();

        Map<ResourceLocation, ArrayImage> ofTextures = ArrayImage.gatherImages(resourceManager, "optifine/colormap");
        Map<ResourceLocation, ArrayImage> cmTextures = ArrayImage.gatherImages(resourceManager, "colormatic/colormap");

        textures.putAll(LegacyHelper.convertPaths(ofTextures));
        textures.putAll(LegacyHelper.convertPaths(cmTextures));

        textures.putAll(ArrayImage.gatherImages(resourceManager, path()));

        return new Resources(jsons, textures);
    }

    @Override
    public void process(Resources resources) {
        var jsons = resources.jsons();
        var textures = resources.textures();

        Set<ResourceLocation> usedTextures = new HashSet<>();

        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            FluidPropertyModifier modifier = FluidPropertyModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Fluid Color Modifier with json id {} - error: {}", id, errorMsg))
                    .getFirst();

            //fill inline colormaps colormapTextures
            BlockColor colormap = modifier.colormap().orElse(null);
            if (colormap instanceof Colormap c) {
                ColormapsManager.tryAcceptingTexture(textures.get(id), id, c, usedTextures);
            }
            addModifier(id, modifier);

        }

        // creates orphaned texture colormaps & properties
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            ResourceLocation id = t.getKey();
            Colormap defaultColormap = Colormap.defTriangle();
            ColormapsManager.tryAcceptingTexture(textures.get(id), id, defaultColormap, usedTextures);

            addModifier(id, new FluidPropertyModifier(Optional.of(defaultColormap),
                    Optional.empty(), Optional.empty()));
        }
    }

    @Override
    protected void reset() {
        modifiers.clear();
        clearSpecial();
    }

    private void addModifier(ResourceLocation pathId, FluidPropertyModifier mod) {

        var explTargets = mod.explicitTargets();
        Optional<Fluid> implicitTarget = Registry.FLUID.getOptional(pathId);
        if (explTargets.isPresent()) {
            if (implicitTarget.isPresent()) {
                Polytone.LOGGER.error("Found Fluid Properties Modifier with Explicit Targets ({}) also having a valid IMPLICIT Path Target ({})." +
                        "Consider moving it under your OWN namespace to avoid overriding other packs modifiers with the same path", explTargets.get(), pathId);
            }
            for (var explicitId : explTargets.get()) {
                Optional<Fluid> target = Registry.FLUID.getOptional(explicitId);
                target.ifPresent(fluid -> modifiers.merge(fluid, mod, FluidPropertyModifier::merge));
                tryAddSpecial(explicitId, mod);
            }
        }
        //no explicit targets. use its own ID instead
        else {
            implicitTarget.ifPresent(block -> modifiers.merge(block, mod, FluidPropertyModifier::merge));
            tryAddSpecial(pathId, mod);
            if (implicitTarget.isEmpty()) {
                Polytone.LOGGER.error("Found Fluid Properties Modifier with no implicit target ({}) and no explicit targets", pathId);
            }
        }
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
        var modifier = modifiers.get(fluidState.getType());
        if (modifier != null) {
            var col = modifier.getColormap();
            if (col != null) {
                return col.getColor(state, level, pos, -1) | 0x44000000;
            }
        }
        return original;
    }

    public FluidPropertyModifier getModifier(Fluid water) {
        return modifiers.get(water);
    }
}
