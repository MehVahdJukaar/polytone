package net.mehvahdjukaar.polytone.fluid;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.utils.*;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.material.Fluid;

import java.util.*;

public class FluidPropertiesManager extends JsonImgPartialReloader {

    private final Map<Fluid, FluidPropertyModifier> modifiers = new HashMap<>();

    public FluidPropertiesManager() {
        super("fluid_modifiers", "fluid_properties");
    }

    private Map<ResourceLocation, FluidPropertyModifier> extraModifiers;
    private Map<ResourceLocation, ArrayImage> extraImages;

    // fot OF lava and water. shit code...
    public void addConvertedBlockProperties(Map<ResourceLocation, FluidPropertyModifier> modifiers, Map<ResourceLocation, ArrayImage> textures) {
        this.extraImages = textures;
        this.extraModifiers = modifiers;
    }

    @Override
    protected Resources prepare(ResourceManager resourceManager) {
        var jsons = this.getJsonsInDirectories(resourceManager);

        Map<ResourceLocation, ArrayImage> textures = new HashMap<>();

        //Map<ResourceLocation, ArrayImage> ofTextures = ArrayImage.gatherImages(resourceManager, "optifine/colormap");
        //LegacyHelper.filterOfFluidTextures(ofTextures);
        Map<ResourceLocation, ArrayImage> cmTextures = ArrayImage.scanDirectory(resourceManager, "colormatic/colormap");

        //textures.putAll(LegacyHelper.convertPaths(ofTextures));
        textures.putAll(LegacyHelper.convertPaths(cmTextures));

        textures.putAll(this.getImagesInDirectories(resourceManager));

        return new Resources(ImmutableMap.copyOf(jsons), ImmutableMap.copyOf(textures));
    }

    //TODO: this is a mess. Improve

    @Override
    protected void parseWithLevel(Resources resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        var jsons = resources.jsons();
        var textures = new HashMap<>(resources.textures());

        Set<ResourceLocation> usedTextures = new HashSet<>();

        Map<ResourceLocation, Parsed<FluidPropertyModifier>> parsedModifiers = new HashMap<>();
        for (var j : extraModifiers.entrySet()) {
            parsedModifiers.put(j.getKey(), Parsed.of(j.getValue(), j.getKey()));
        }
        textures.putAll(extraImages);


        for (var j : jsons.entrySet()) {
            JsonElement json = j.getValue();
            ResourceLocation id = j.getKey();

            var modifier = Parsed.parseFull(FluidPropertyModifier.CODEC, json, ops, id, "fluid modifier");

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
            Parsed<FluidPropertyModifier> parsed = entry.getValue();
            FluidPropertyModifier modifier = parsed.getResultOrPartial();

            if (!modifier.hasColormap() && textures.containsKey(id)) {
                //if this map doesn't have a colormap defined, we set it to the default impl IF there's a texture it can use
                modifier = modifier.merge(FluidPropertyModifier.ofBlockColor(Colormap.createDefTriangle()));
            }

            //fill inline colormaps colormapTextures
            BlockColor tint = modifier.getTint();
            ColormapsManager.tryAcceptingTexture(textures, id, tint, usedTextures, true);

            parsed.ifEnabled(this::addModifier);
        }

        // creates orphaned texture colormaps & properties
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            ResourceLocation id = t.getKey();
            Colormap defaultColormap = Colormap.createDefTriangle();
            ColormapsManager.tryAcceptingTexture(textures, id, defaultColormap, usedTextures, true);

            addModifier(id, new FluidPropertyModifier(Optional.of(defaultColormap),
                    Optional.empty(), Targets.EMPTY));
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
        if (!modifiers.isEmpty()) {
            Polytone.LOGGER.info("Applied {} Fluid Modifiers", modifiers.size());
        }
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        modifiers.clear();
        clearSpecial();
    }

    private void addModifier(ResourceLocation pathId, FluidPropertyModifier mod) {
        for (var fluid : mod.targets().compute(pathId, BuiltInRegistries.FLUID)) {
            var f = fluid.value();
            modifiers.merge(f, mod, FluidPropertyModifier::merge);
            tryAddSpecial(f, mod);
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
