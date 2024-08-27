package net.mehvahdjukaar.polytone.fluid;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.block.BlockPropertyModifier;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.mehvahdjukaar.polytone.utils.LegacyHelper;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.material.Fluid;

import java.util.*;

public class FluidPropertiesManager extends JsonImgPartialReloader {

    private final Map<Fluid, FluidPropertyModifier> modifiers = new HashMap<>();

    public FluidPropertiesManager() {
        super( "fluid_modifiers", "fluid_properties");
    }

    private Map<ResourceLocation, FluidPropertyModifier> extraModifiers;
    private Map<ResourceLocation, ArrayImage> extraImages;

    // fot OF lava and water. shit code...
    public void addConvertedBlockProperties(Map<ResourceLocation, FluidPropertyModifier> modifiers, Map<ResourceLocation, ArrayImage> textures) {
        this.extraImages = textures;
        this.extraModifiers =  modifiers;
    }

    @Override
    protected Resources prepare(ResourceManager resourceManager) {
        var jsons = this.getJsonsInDirectories(resourceManager);
        this.checkConditions(jsons);

        Map<ResourceLocation, ArrayImage> textures = new HashMap<>();

        //Map<ResourceLocation, ArrayImage> ofTextures = ArrayImage.gatherImages(resourceManager, "optifine/colormap");
        //LegacyHelper.filterOfFluidTextures(ofTextures);
        Map<ResourceLocation, ArrayImage> cmTextures = ArrayImage.scanDirectory(resourceManager, "colormatic/colormap");

        //textures.putAll(LegacyHelper.convertPaths(ofTextures));
        textures.putAll(LegacyHelper.convertPaths(cmTextures));

        textures.putAll(this.getImagesInDirectories(resourceManager));

        return new Resources(jsons, textures);
    }

    //TODO: this is a mess. Improve
    @Override
    public void process(Resources resources, DynamicOps<JsonElement> ops) {
        var jsons = resources.jsons();
        var textures = resources.textures();

        Set<ResourceLocation> usedTextures = new HashSet<>();

        Map<ResourceLocation, FluidPropertyModifier> parsedModifiers = new HashMap<>(extraModifiers);
        textures.putAll(extraImages);


        for (var j : jsons.entrySet()) {
            JsonElement json = j.getValue();
            ResourceLocation id = j.getKey();

            FluidPropertyModifier modifier = FluidPropertyModifier.CODEC.decode(ops, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Fluid Modifier with json id " + id + "\n error: " + errorMsg))
                    .getFirst();

            //always have priority
            if (parsedModifiers.containsKey(id)) {
                Polytone.LOGGER.warn("Found duplicate fluid modifier with id {}. This is likely a non .json converted legacy one" +
                        "Overriding previous one", id);
            }
            parsedModifiers.put(id, modifier);
        }

        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers.entrySet()) {
            ResourceLocation id = entry.getKey();
            FluidPropertyModifier modifier = entry.getValue();

            if (!modifier.hasColormap() && textures.containsKey(id)) {
                //if this map doesn't have a colormap defined, we set it to the default impl IF there's a texture it can use
                modifier = modifier.merge(FluidPropertyModifier.ofBlockColor(Colormap.createDefTriangle()));
            }

            //fill inline colormaps colormapTextures
            BlockColor tint = modifier.getTint();
            ColormapsManager.tryAcceptingTexture(textures, id, tint, usedTextures, true);
            addModifier(id, modifier);
        }

        // creates orphaned texture colormaps & properties
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            ResourceLocation id = t.getKey();
            Colormap defaultColormap = Colormap.createDefTriangle();
            ColormapsManager.tryAcceptingTexture(textures, id, defaultColormap, usedTextures, true);

            addModifier(id, new FluidPropertyModifier(Optional.of(defaultColormap),
                    Optional.empty(), Set.of()));
        }
    }

    @Override
    protected void apply() {
        super.apply();
        if (!modifiers.isEmpty())
            Polytone.LOGGER.info("Applied {} Fluid Modifiers", modifiers.size());
    }

    @Override
    protected void reset() {
        modifiers.clear();
        clearSpecial();
    }

    private void addModifier(ResourceLocation pathId, FluidPropertyModifier mod) {
        for (Fluid fluid : mod.getTargets(pathId, BuiltInRegistries.FLUID)) {
            modifiers.merge(fluid, mod, FluidPropertyModifier::merge);
            tryAddSpecial(fluid, mod);
        }
    }

    @ExpectPlatform
    private static void tryAddSpecial(Fluid fluid, FluidPropertyModifier colormap) {
        throw new AssertionError();
    }

    @ExpectPlatform
    private static void clearSpecial() {
        throw new AssertionError();
    }


    public FluidPropertyModifier getModifier(Fluid water) {
        return modifiers.get(water);
    }

}
