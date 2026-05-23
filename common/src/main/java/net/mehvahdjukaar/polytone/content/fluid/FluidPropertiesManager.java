package net.mehvahdjukaar.polytone.content.fluid;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.common.LegacyHelper;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.Targets;
import net.mehvahdjukaar.polytone.common.struc.ArrayImage;
import net.mehvahdjukaar.polytone.common.reloader.JsonImgPartialReloader;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.*;

public class FluidPropertiesManager extends JsonImgPartialReloader {

    private final Map<Fluid, FluidPropertyModifier> modifiers = new HashMap<>();

    public FluidPropertiesManager() {
        super("fluid_modifiers", "fluid_properties");
    }

    private Map<Identifier, Parsed<FluidPropertyModifier>> extraModifiers;
    private Map<Identifier, ArrayImage> extraImages;

    //essentially replacing this for better mod compat
    private ColorResolver vanillaWaterColorResolver = null;

    // fot OF lava and water. shit code...
    public void addConvertedBlockProperties(Map<Identifier, Parsed<FluidPropertyModifier>> modifiers, Map<Identifier, ArrayImage> textures) {
        this.extraImages = textures;
        this.extraModifiers = modifiers;
    }

    @Override
    protected Resources prepare(PreparableReloadListener.SharedState sharedState) {
        var resourceManager = sharedState.resourceManager();
        var jsons = this.getJsonsInDirectories(resourceManager);

        Map<Identifier, ArrayImage> textures = new HashMap<>();

        //Map<Identifier, ArrayImage> ofTextures = ArrayImage.gatherImages(resourceManager, "optifine/colormap");
        //LegacyHelper.filterOfFluidTextures(ofTextures);
        Map<Identifier, ArrayImage> cmTextures = ArrayImage.scanDirectory(resourceManager, "colormatic/colormap");

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

        Set<Identifier> usedTextures = new HashSet<>();

        LinkedListMultimap<Identifier, Parsed<FluidPropertyModifier>> parsedModifiers =   LinkedListMultimap.create();
        extraModifiers.forEach(parsedModifiers::put);
        textures.putAll(extraImages);


        for (var j : Parsed.batchParseAlways(jsons, FluidPropertyModifier.CODEC, ops, "fluid modifier")) {
            Identifier id = j.getKey();
            parsedModifiers.put(id, j.getValue());
        }

        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers.entries()) {
            Identifier id = entry.getKey();
            Parsed<FluidPropertyModifier> parsed = entry.getValue();
            FluidPropertyModifier modifier = parsed.getResultOrPartial();

            if (!modifier.hasColormap() && textures.containsKey(id)) {
                //if this map doesn't have a colormap defined, we set it to the default impl IF there's a texture it can use
                modifier = modifier.merge(FluidPropertyModifier.ofBlockColor(Colormap.createDefTriangle()));
            }

            //fill inline colormaps colormapTextures
            IColorGetter tint = modifier.getColormap();
            ColormapsManager.tryAcceptingTexture(textures, id, tint, usedTextures, true);

            if (parsed.isEnabled()) this.addModifier(id, modifier);
        }

        // creates orphaned texture colormaps & properties
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            Identifier id = t.getKey();
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
        if (vanillaWaterColorResolver != null) {
            BiomeColors.WATER_COLOR_RESOLVER = vanillaWaterColorResolver;
        }
        vanillaWaterColorResolver = null;
    }

    private void addModifier(Identifier pathId, FluidPropertyModifier mod) {
        for (var fluid : mod.targets().compute(pathId, BuiltInRegistries.FLUID)) {
            Fluid f = fluid.value();
            modifiers.merge(f, mod, FluidPropertyModifier::merge);
            tryAddSpecial(f, mod);

            //replaces watercolor func with first colormap that targets water. good enough
            if (fluid.value() == Fluids.WATER && mod.getColormap() instanceof ColorResolver c) {
                vanillaWaterColorResolver = BiomeColors.WATER_COLOR_RESOLVER;
                BiomeColors.WATER_COLOR_RESOLVER = c;
            }
        }
    }

    @PlatformImpl
    private static void tryAddSpecial(Fluid fluid, FluidPropertyModifier colormap) {
        throw new AssertionError();
    }

    @PlatformImpl
    private static void clearSpecial() {
        throw new AssertionError();
    }


    public FluidPropertyModifier getModifier(Fluid water) {
        return modifiers.get(water);
    }

}
