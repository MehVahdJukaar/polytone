package net.mehvahdjukaar.polytone.dimension;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
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


    public DimensionEffectsManager() {
        super("dimension_effects");
    }

    @Override
    public void reset() {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            for (var v : vanillaEffects.entrySet()) {
                v.getValue().applyInplace(v.getKey());
            }
            //reset all
        }
        //if we don't have a level, biomes don't exist anymore, so we don't care

        vanillaEffects.clear();

        //whatever happens, we always clear stuff to apply
        effectsToApply.clear();
        fogColormaps.clear();
        skyColormaps.clear();
    }


    @Override
    protected void process(Resources resources) {
        var jsons = resources.jsons();
        var textures = resources.textures();

        Set<ResourceLocation> usedTextures = new HashSet<>();

        Map<ResourceLocation, DimensionEffectsModifier> parsedModifiers = new HashMap<>();


        for (var j : jsons.entrySet()) {
            JsonElement json = j.getValue();
            ResourceLocation id = j.getKey();

            DimensionEffectsModifier modifier = DimensionEffectsModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Dimension Effects with json id " + id + " - error: " + errorMsg))
                    .getFirst();

            //always have priority
            if (parsedModifiers.containsKey(id)) {
                Polytone.LOGGER.warn("Found duplicate Dimension Effects file with id {}. This is likely a non .json converted legacy one" +
                        "Overriding previous one", id);
            }
            parsedModifiers.put(id, modifier);
        }

        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers.entrySet()) {
            ResourceLocation id = entry.getKey();
            DimensionEffectsModifier modifier = entry.getValue();

            if (!modifier.hasFogColormap() && textures.containsKey(id)) {
                //if this map doesn't have a colormap defined, we set it to the default impl IF there's a texture it can use
                modifier = modifier.merge(DimensionEffectsModifier.ofFogColor(Colormap.defTriangle()));
            }

            //fill inline colormaps colormapTextures
            if (modifier.hasFogColormap()) {
                BlockColor tint = modifier.getFogColormap();
                if (tint instanceof Colormap c) {
                    var text = textures.get(c.getTargetTexture() == null ? id : c.getTargetTexture());
                    if (text != null) {
                        ColormapsManager.tryAcceptingTexture(text, id, c, usedTextures);
                    } else if (c.getTargetTexture() != null) {
                        Polytone.LOGGER.error("Could not resolve explicit texture at location {}.png for colormap from dimension effects file with it {}. Skipping", c.getTargetTexture(), id);
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
        if (firstLogin) vanillaEffects.clear();

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
                    //int fogColor = biome.getFogColor();
                    int fogColor1 = colormap.sampleColor(null, BlockPos.containing(qx * 4, qy * 4, qz * 4), biome); //quart coords to block coord
                    return Vec3.fromRGB24(fogColor1);
                }), brightness);
    }

    @Nullable
    public Vec3 modifySkyColor(Vec3 center, ClientLevel level) {
        Colormap colormap = this.skyColormaps.get(level.dimensionType());
        if (colormap == null) return null;

        BiomeManager biomeManager = level.getBiomeManager();
        return CubicSampler.gaussianSampleVec3(center, (qx, qy, qz) -> {
            var biome = biomeManager.getNoiseBiomeAtQuart(qx, qy, qz).value();
            //int skyColor = biome.getSkyColor();
            int skyColor1 = colormap.sampleColor(null, BlockPos.containing(qx * 4, qy * 4, qz * 4), biome); //quart coords to block coord
            return Vec3.fromRGB24(skyColor1);
        });
    }
}
