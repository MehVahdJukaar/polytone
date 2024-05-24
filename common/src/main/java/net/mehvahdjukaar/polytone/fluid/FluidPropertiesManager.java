package net.mehvahdjukaar.polytone.fluid;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.block.BlockPropertyModifier;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.colormap.CompoundBlockColors;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.mehvahdjukaar.polytone.utils.LegacyHelper;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.material.Fluid;

import java.util.*;

import static net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener.scanDirectory;

public class FluidPropertiesManager extends JsonImgPartialReloader {

    private final Map<Fluid, FluidPropertyModifier> modifiers = new HashMap<>();

    public FluidPropertiesManager() {
        super("fluid_properties");
    }

    private Map<ResourceLocation, FluidPropertyModifier> extraModifiers;
    private Map<ResourceLocation, ArrayImage> extraImages;

    // fot OF lava and water. shit code...
    public void addConvertedBlockProperties(Map<ResourceLocation, BlockPropertyModifier> modifiers, Map<ResourceLocation, ArrayImage> textures) {
        this.extraImages = textures;
        extraModifiers = new HashMap<>();
        for (var v : modifiers.entrySet()) {
            var m = v.getValue();
            var c = m.tintGetter();
            if (c.isPresent()) {
                // ignore targets as those are block targets anyways
                extraModifiers.put(v.getKey(), new FluidPropertyModifier((Optional<BlockColor>) c, Optional.empty(), m.explicitTargets()));
            }
        }
    }

    @Override
    protected Resources prepare(ResourceManager resourceManager) {
        Map<ResourceLocation, JsonElement> jsons = new HashMap<>();
        scanDirectory(resourceManager, path(), GSON, jsons);
        checkConditions(jsons);

        Map<ResourceLocation, ArrayImage> textures = new HashMap<>();

        //Map<ResourceLocation, ArrayImage> ofTextures = ArrayImage.gatherImages(resourceManager, "optifine/colormap");
        //LegacyHelper.filterOfFluidTextures(ofTextures);
        Map<ResourceLocation, ArrayImage> cmTextures = ArrayImage.gatherImages(resourceManager, "colormatic/colormap");

        //textures.putAll(LegacyHelper.convertPaths(ofTextures));
        textures.putAll(LegacyHelper.convertPaths(cmTextures));

        textures.putAll(ArrayImage.gatherImages(resourceManager, path()));

        return new Resources(jsons, textures);
    }

    //TODO: this is a mess. Improve
    @Override
    public void process(Resources resources) {
        var jsons = resources.jsons();
        var textures = resources.textures();

        Set<ResourceLocation> usedTextures = new HashSet<>();

        Map<ResourceLocation, FluidPropertyModifier> parsedModifiers = new HashMap<>(extraModifiers);
        textures.putAll(extraImages);


        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            FluidPropertyModifier modifier = FluidPropertyModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Fluid Color Modifier with json id " + id + "\n error: " + errorMsg))
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
            var id = entry.getKey();
            var modifier = entry.getValue();

            var colormap = modifier.colormap();
            if (colormap.isEmpty()) {
                //if this map doesn't have a colormap defined, we set it to the default impl IF there's a texture it can use

                var text = textures.get(id);
                if (text != null) {
                    modifier = modifier.merge(FluidPropertyModifier.ofBlockColor(Colormap.defTriangle()));
                    colormap = modifier.colormap();
                }
            }

            //fill inline colormaps colormapTextures
            if (colormap.isPresent()) {
                BlockColor tint = colormap.get();
                if (tint instanceof Colormap c) {
                    var text = textures.get(c.getTargetTexture() == null ? id : c.getTargetTexture());
                    if (text != null) {
                        ColormapsManager.tryAcceptingTexture(text, id, c, usedTextures);
                    } else if (c.getTargetTexture() != null) {
                        Polytone.LOGGER.error("Could not resolve explicit texture at location {}.png for colormap from fluid modifier {}. Skipping", c.getTargetTexture(), id);
                        continue;
                    }
                }
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
        Optional<Fluid> implicitTarget = BuiltInRegistries.FLUID.getOptional(pathId);
        if (explTargets.isPresent() && !explTargets.get().isEmpty()) {
            if (implicitTarget.isPresent() && !explTargets.get().contains(pathId)) {
                Polytone.LOGGER.error("Found Fluid Properties Modifier with Explicit Targets ({}) also having a valid IMPLICIT Path Target ({})." +
                        "Consider moving it under your OWN namespace to avoid overriding other packs modifiers with the same path", explTargets.get(), pathId);
            }
            for (var explicitId : explTargets.get()) {
                Optional<Fluid> target = BuiltInRegistries.FLUID.getOptional(explicitId);
                target.ifPresent(fluid -> modifiers.merge(fluid, mod, FluidPropertyModifier::merge));
                tryAddSpecial(explicitId, mod);
            }
        }
        //no explicit targets. use its own ID instead
        else {
            implicitTarget.ifPresent(fluid -> modifiers.merge(fluid, mod, FluidPropertyModifier::merge));
            tryAddSpecial(pathId, mod);
            if (implicitTarget.isEmpty()) {
                if (PlatStuff.isModLoaded(pathId.getNamespace())) {
                    Polytone.LOGGER.error("Found Fluid Properties Modifier with no implicit target ({}) and no explicit targets", pathId);
                }
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


    public FluidPropertyModifier getModifier(Fluid water) {
        return modifiers.get(water);
    }

}
