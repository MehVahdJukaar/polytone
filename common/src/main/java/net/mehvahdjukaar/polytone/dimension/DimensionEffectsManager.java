package net.mehvahdjukaar.polytone.dimension;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CubicSampler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DimensionEffectsManager extends JsonImgPartialReloader {

    private final Map<ResourceLocation, DimensionEffectsModifier> effectsToApply = new HashMap<>();

    private final Map<ResourceLocation, DimensionEffectsModifier> vanillaEffects = new HashMap<>();

    private final Map<DimensionType, Colormap> fogColormaps = new HashMap<>();
    private final Map<DimensionType, Colormap> skyColormaps = new HashMap<>();

    private boolean needsDynamicApplication = true;

    private final Map<ResourceLocation, DimensionEffectsModifier> extraMods = new HashMap<>();

    public DimensionEffectsManager() {
        super("dimension_modifiers", "dimension_effects");
    }

    @Override
    public void reset() {
        needsDynamicApplication = true;

        //Dimensions are NOT reloaded with world load. we need to reset vanilla stuff once we have a level
        //whatever happens, we always clear stuff to apply
        effectsToApply.clear();
        fogColormaps.clear();
        skyColormaps.clear();
        extraMods.clear();
    }


    @Override
    protected void process(Resources resources, DynamicOps<JsonElement> ops) {
        var jsons = resources.jsons();
        var textures = resources.textures();

        Set<ResourceLocation> usedTextures = new HashSet<>();

        Map<ResourceLocation, DimensionEffectsModifier> parsedModifiers = new HashMap<>(extraMods);

        for (var j : jsons.entrySet()) {
            JsonElement json = j.getValue();
            ResourceLocation id = j.getKey();

            DimensionEffectsModifier modifier = DimensionEffectsModifier.CODEC.decode(ops, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Dimension Effects with json id " + id + " - error: " + errorMsg))
                    .getFirst();

            //always have priority
            if (parsedModifiers.containsKey(id)) {
                Polytone.LOGGER.warn("Found duplicate Dimension Effects file with id {}." +
                        "Overriding previous one", id);
            }
            parsedModifiers.put(id, modifier);
        }

        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers.entrySet()) {
            ResourceLocation id = entry.getKey();
            DimensionEffectsModifier modifier = entry.getValue();

            BlockColor fog = modifier.getFogColormap();
            BlockColor sky = modifier.getSkyColormap();

            //adds implicit single texture
            // just 1 colormap defined
            if (textures.containsKey(id) && fog == null && sky == null) {
                //if this map doesn't have any colormaps but has a texture we give it fog color
                modifier = modifier.merge(DimensionEffectsModifier.ofFogColor(Colormap.createDefTriangle()));
                fog = modifier.getFogColormap();
            }

            // if sky is not defined BUT they have a valid texture create a colormap for it
            ResourceLocation skyId = id.withSuffix("_sky");
            if (textures.containsKey(skyId) && sky == null) {
                modifier = modifier.merge(DimensionEffectsModifier.ofSkyColor(Colormap.createDefTriangle()));
                sky = modifier.getSkyColormap();
            }

            // if fog is not defined BUT they have a valid texture create a colormap for it
            ResourceLocation fogId = id.withSuffix("_fog");
            if (textures.containsKey(fogId) && sky == null) {
                modifier = modifier.merge(DimensionEffectsModifier.ofFogColor(Colormap.createDefTriangle()));
                fog = modifier.getFogColormap();
            }

            //fill textures

            // if just one of them is defined we try assigning to the deafult texture at id
            if ((fog != null) ^ (sky != null)) {
                ColormapsManager.tryAcceptingTexture(textures, id, fog, usedTextures, false);
                ColormapsManager.tryAcceptingTexture(textures, id, sky, usedTextures, false);
            }

            //try filling with standard textures paths, failing if they are not there
            ColormapsManager.tryAcceptingTexture(textures, fogId, fog, usedTextures, true);
            ColormapsManager.tryAcceptingTexture(textures, skyId, sky, usedTextures, true);

            addModifier(id, modifier);
        }

        // creates orphaned texture colormaps & properties
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            ResourceLocation id = t.getKey();
            Colormap defaultColormap = Colormap.createDefTriangle();
            ColormapsManager.tryAcceptingTexture(textures, id, defaultColormap, usedTextures, true);

            addModifier(id, DimensionEffectsModifier.ofFogColor(defaultColormap));
        }
    }

    private void addModifier(ResourceLocation fileId, DimensionEffectsModifier mod) {
        for (ResourceLocation id : mod.getTargetsKeys(fileId)) {
            effectsToApply.merge(id, mod, DimensionEffectsModifier::merge);
        }
    }

    @Override
    public void apply() {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            doApply(level.registryAccess(), false);
        }
        //else apply as soon as we load a level
    }

    public void doApply(RegistryAccess registryAccess, boolean firstLogin) {
        if (!firstLogin && !needsDynamicApplication) return;
        needsDynamicApplication = false;

        for (var v : vanillaEffects.entrySet()) {
            v.getValue().applyInplace(v.getKey());
        }

        Registry<DimensionType> dimReg = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE);

        for (var v : effectsToApply.entrySet()) {
            ResourceLocation dimensionId = v.getKey();
            DimensionEffectsModifier modifier = v.getValue();
            var old = modifier.applyInplace(dimensionId);

            vanillaEffects.put(dimensionId, old);

            if (modifier.getFogColormap() instanceof Colormap c) {
                fogColormaps.put(dimReg.get(dimensionId), c);
            }
            if (modifier.getSkyColormap() instanceof Colormap c) {
                skyColormaps.put(dimReg.get(dimensionId), c);
            }
        }
        if (!vanillaEffects.isEmpty())
            Polytone.LOGGER.info("Applied {} Custom Dimension Effects Properties", vanillaEffects.size());
        //we don't clear effects to apply because we need to re apply on world reload

    }

    @Nullable
    public Vec3 modifyFogColor(Vec3 center, ClientLevel level, float brightness) {
        Colormap colormap = this.fogColormaps.get(level.dimensionType());
        if (colormap == null) return null;
        BiomeManager biomeManager = level.getBiomeManager();
        return level.effects().getBrightnessDependentFogColor(
                CubicSampler.gaussianSampleVec3(center, (qx, qy, qz) -> {
                    var biome = biomeManager.getNoiseBiomeAtQuart(qx, qy, qz).value();
                    // will override all biome modifiers ones
                    //int fogColor = biome.getFogColor();
                    int fogColor1 = colormap.sampleColor(null,
                            BlockPos.containing(qx * 4, qy * 4, qz * 4), biome, null); //quart coords to block coord
                    return Vec3.fromRGB24(fogColor1);
                }), brightness);
    }

    public void modifyFogMagicNumber(float renderDistanceChunks, LocalFloatRef distance) {
        //no more random sky seam!
        float c = 0.25f;
        float b = c + (1 - c) * renderDistanceChunks / 32.0F;
        b = 1.0F - (float) Math.pow(b, 0.25);
        float a = 1 * renderDistanceChunks / 32.0F;
        a = 1.0F - (float) Math.pow(a, 0.25);
        distance.set(b);
    }

    @Nullable
    public Vec3 modifySkyColor(Vec3 center, ClientLevel level) {
        Colormap colormap = this.skyColormaps.get(level.dimensionType());
        if (colormap == null) return null;

        BiomeManager biomeManager = level.getBiomeManager();
        return CubicSampler.gaussianSampleVec3(center, (qx, qy, qz) -> {
            var biome = biomeManager.getNoiseBiomeAtQuart(qx, qy, qz).value();
            //int skyColor = biome.getSkyColor();
            int skyColor1 = colormap.sampleColor(null, BlockPos.containing(qx * 4, qy * 4, qz * 4), biome, null); //quart coords to block coord
            return Vec3.fromRGB24(skyColor1);
        });
    }

    public void addConvertedBlockProperties(Map<ResourceLocation, DimensionEffectsModifier> converted) {
        extraMods.clear();
        extraMods.putAll(converted);
    }
}
