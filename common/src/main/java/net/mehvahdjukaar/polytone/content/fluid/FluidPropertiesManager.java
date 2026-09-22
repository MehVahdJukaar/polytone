package net.mehvahdjukaar.polytone.content.fluid;

import com.google.common.collect.LinkedListMultimap;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.companion.TextureRole;
import net.mehvahdjukaar.polytone.common.companion.ScannedTextures;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
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
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FluidPropertiesManager extends ContentManager<FluidPropertyModifier> {

    private final Map<Fluid, FluidPropertyModifier> modifiers = new HashMap<>();
    private final Map<Fluid, IColorGetter> concurrentTints = new ConcurrentHashMap<>();

    private static final TextureRole<FluidPropertyModifier> TINT =
            TextureRole.plain("tint", FluidPropertyModifier::getColormap);
    private static final TextureRole<FluidPropertyModifier> FOG =
            TextureRole.suffix("_fog", FluidPropertyModifier::getFogColormap);


    public FluidPropertiesManager() {
        super(Spec.of("Fluid modifier", () -> FluidPropertyModifier.CODEC)
                .wikiPage("Fluid-Properties-Modifiers")
                .textureParts(TINT, FOG)
                .folders("fluid_modifiers", "fluid_properties"));
    }

    private static FluidPropertyModifier defaultFor(TextureRole<FluidPropertyModifier> part) {
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
        var textures = new ScannedTextures(resources.textures());

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
        for (var entry : modifiers.entrySet()) {
            Fluid fluid = entry.getKey();
            IColorGetter tint = entry.getValue().getColormap();
            if (tint == null) continue;
            IColorGetter concurrent = Polytone.COLORMAPS.getOrCreateConcurrentColormap(tint);
            concurrentTints.put(fluid, concurrent);
            if (fluid instanceof FlowingFluid ff) {
                concurrentTints.putIfAbsent(ff.getSource(), concurrent);
                concurrentTints.putIfAbsent(ff.getFlowing(), concurrent);
            }
        }
        if (!modifiers.isEmpty()) {
            Polytone.LOGGER.info("Applied {} Fluid Modifiers", modifiers.size());
        }
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        modifiers.clear();
        concurrentTints.clear();
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

    @Nullable
    public IColorGetter getConcurrentTint(Fluid fluid) {
        return concurrentTints.get(fluid);
    }

    public boolean hasAnyModifier() {
        return !modifiers.isEmpty();
    }

    // a pack usually only targets one of the still/flowing pair, so accept either one here
    @Nullable
    public FluidPropertyModifier getModifierOrVariant(Fluid fluid) {
        FluidPropertyModifier mod = modifiers.get(fluid);
        if (mod == null && fluid instanceof FlowingFluid ff) {
            mod = modifiers.get(ff.getSource());
            if (mod == null) mod = modifiers.get(ff.getFlowing());
        }
        return mod;
    }

}
