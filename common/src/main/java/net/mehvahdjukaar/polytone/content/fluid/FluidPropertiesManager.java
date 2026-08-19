package net.mehvahdjukaar.polytone.content.fluid;

import com.google.common.collect.LinkedListMultimap;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.companion.TexturePart;
import net.mehvahdjukaar.polytone.common.companion.TrackedTextures;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.common.LegacyHelper;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.struc.ArrayImage;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
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

public class FluidPropertiesManager extends ContentManager<FluidPropertyModifier> {

    private final Map<Fluid, FluidPropertyModifier> modifiers = new HashMap<>();

    private static final TexturePart<FluidPropertyModifier> TINT =
            TexturePart.plain("tint", FluidPropertyModifier::getColormap);
    private static final TexturePart<FluidPropertyModifier> FOG =
            TexturePart.suffix("_fog", FluidPropertyModifier::getFogColormap);

    public FluidPropertiesManager() {
        super(Spec.of("Fluid modifier", () -> FluidPropertyModifier.CODEC)
                .wikiPage("Fluid-Properties-Modifiers")
                .textureParts(TINT, FOG)
                .folders("fluid_modifiers", "fluid_properties"));
    }

    private static FluidPropertyModifier defaultFor(TexturePart<FluidPropertyModifier> part) {
        return part == FOG ? FluidPropertyModifier.ofFogColor(Colormap.createDefTriangle())
                : FluidPropertyModifier.ofBlockColor(Colormap.createDefTriangle());
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
    protected AssetsFiles prepare(PreparableReloadListener.SharedState sharedState) {
        var resourceManager = sharedState.resourceManager();
        var jsons = this.getJsonsInDirectories(resourceManager);

        Map<Identifier, ArrayImage> textures = new HashMap<>();

        //Map<Identifier, ArrayImage> ofTextures = ArrayImage.gatherImages(resourceManager, "optifine/colormap");
        //LegacyHelper.filterOfFluidTextures(ofTextures);
        Map<Identifier, ArrayImage> cmTextures = ArrayImage.scanDirectory(resourceManager, "colormatic/colormap");

        //textures.putAll(LegacyHelper.convertPaths(ofTextures));
        textures.putAll(LegacyHelper.convertPaths(cmTextures));

        textures.putAll(this.getImagesInDirectories(resourceManager));

        return new AssetsFiles(jsons, textures);
    }

    //TODO: this is a mess. Improve

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        var jsons = resources.jsons();
        var textures = new TrackedTextures(resources.textures());

        LinkedListMultimap<Identifier, Parsed<FluidPropertyModifier>> parsedModifiers =   LinkedListMultimap.create();
        extraModifiers.forEach(parsedModifiers::put);
        textures.putAll(extraImages);


        for (var j : parseAllJsons(jsons, ops)) {
            Identifier id = j.getKey();
            parsedModifiers.put(id, j.getValue());
        }

        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers.entries()) {
            Identifier id = entry.getKey();
            Parsed<FluidPropertyModifier> parsed = entry.getValue();
            FluidPropertyModifier modifier = parsed.getResultOrPartial();

            // auto-attach defaults for lone textures, then fill inline colormaps from the scanned ones
            for (var part : contentTexture.adoptable(textures, id, modifier).keySet()) {
                modifier = modifier.merge(defaultFor(part));
            }
            contentTexture.fill(textures, id, modifier, true);

            if (parsed.isEnabled()) this.addModifier(id, modifier);
        }

        // creates orphaned texture colormaps & properties
        for (var orphan : contentTexture.orphans(textures, parsedModifiers.keySet())) {
            FluidPropertyModifier modifier = null;
            for (var part : orphan.parts().keySet()) {
                FluidPropertyModifier d = defaultFor(part);
                modifier = modifier == null ? d : modifier.merge(d);
            }
            contentTexture.fill(textures, orphan.stemId(), modifier, true);
            addModifier(orphan.stemId(), modifier);
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
