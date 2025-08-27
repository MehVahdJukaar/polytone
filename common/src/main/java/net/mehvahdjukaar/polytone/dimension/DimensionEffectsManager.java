package net.mehvahdjukaar.polytone.dimension;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.block.BlockContextExpression;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.utils.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import java.util.*;

public class DimensionEffectsManager extends JsonImgPartialReloader {

    private final Map<ResourceLocation, DimensionEffectsModifier> effectsToApply = new HashMap<>();

    private final Map<ResourceLocation, DimensionEffectsModifier> vanillaEffects = new HashMap<>();

    private final Object2ObjectMap<DimensionType, IColorGetter> fogColormaps = new Object2ObjectArrayMap<>();
    private final Object2ObjectMap<DimensionType, IColorGetter> terrainFogColormaps = new Object2ObjectArrayMap<>();
    private final Object2ObjectMap<DimensionType, IColorGetter> skyColormaps = new Object2ObjectArrayMap<>();
    private final Object2ObjectMap<DimensionType, IColorGetter> sunsetColormaps = new Object2ObjectArrayMap<>();
    private final Object2ObjectMap<DimensionType, BlockContextExpression> cloudFunctions = new Object2ObjectArrayMap<>();
    private final Object2BooleanArrayMap<DimensionType> cancelFogWeatherDarken = new Object2BooleanArrayMap<>();
    private final Object2BooleanArrayMap<DimensionType> cancelSkyWeatherDarken = new Object2BooleanArrayMap<>();

    private boolean needsDynamicApplication = true;

    private final Map<ResourceLocation, Parsed<DimensionEffectsModifier>> extraMods = new HashMap<>();

    public DimensionEffectsManager() {
        super("dimension_modifiers", "dimension_effects");
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        needsDynamicApplication = true;

        //Dimensions are NOT reloaded with world load. we need to reset vanilla stuff once we have a level
        //whatever happens, we always clear stuff to apply
        effectsToApply.clear();
        fogColormaps.clear();
        skyColormaps.clear();
        sunsetColormaps.clear();
        cancelFogWeatherDarken.clear();
        cancelSkyWeatherDarken.clear();
        cloudFunctions.clear();
        extraMods.clear();
    }

    @Override
    protected void parseWithLevel(Resources resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        var jsons = resources.jsons();
        var textures = new HashMap<>(resources.textures());

        Set<ResourceLocation> usedTextures = new HashSet<>();

        Parsed.SortedMap<DimensionEffectsModifier> parsedModifiers =
                Parsed.batchParseAlways(jsons, DimensionEffectsModifier.CODEC, ops, "dimension modifier");
        parsedModifiers.putAll(extraMods);

        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers) {
            ResourceLocation id = entry.getKey();
            Parsed<DimensionEffectsModifier> parsed = entry.getValue();
            DimensionEffectsModifier modifier = parsed.getResultOrPartial();

            BlockColor fog = modifier.getFogColormap();
            BlockColor sky = modifier.getSkyColormap();
            BlockColor sunset = modifier.getSunsetColormap();
            BlockColor terrainFog = modifier.getTerrainFogColormap();

            //adds implicit single texture
            // just 1 colormap defined. gives to fog
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

            // if sunset is not defined BUT they have a valid texture create a colormap for it
            ResourceLocation sunsetId = id.withSuffix("_sunset");
            if (textures.containsKey(sunsetId) && sunset == null) {
                modifier = modifier.merge(DimensionEffectsModifier.ofSunsetColor(Colormap.createTimeStrip()));
                sunset = modifier.getSunsetColormap();
            }

            // if terrain fog is not defined BUT they have a valid texture create a colormap for it
            ResourceLocation terrainFogId = id.withSuffix("_terrain_fog");
            if (textures.containsKey(terrainFogId) && terrainFog == null) {
                modifier = modifier.merge(DimensionEffectsModifier.ofFogColor(Colormap.createDefTriangle()));
                terrainFog = modifier.getTerrainFogColormap();
            }

            //fill textures

            // if just one of them is defined we try assigning to the deafult texture at id
            if ((fog != null) ^ (sky != null) ^ (sunset != null) ^ (terrainFog != null)) {
                ColormapsManager.tryAcceptingTexture(textures, id, fog, usedTextures, false);
                ColormapsManager.tryAcceptingTexture(textures, id, sky, usedTextures, false);
                ColormapsManager.tryAcceptingTexture(textures, id, sunset, usedTextures, false);
            }

            //try filling with standard textures paths, failing if they are not there
            ColormapsManager.tryAcceptingTexture(textures, fogId, fog, usedTextures, true);
            ColormapsManager.tryAcceptingTexture(textures, skyId, sky, usedTextures, true);
            ColormapsManager.tryAcceptingTexture(textures, sunsetId, sunset, usedTextures, true);
            ColormapsManager.tryAcceptingTexture(textures, terrainFogId, terrainFog, usedTextures, true);

            if (parsed.isEnabled()) {
                addModifier(id, modifier, access);
            }
        }

        // creates orphaned texture colormaps & properties
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            ResourceLocation id = t.getKey();
            Colormap defaultColormap = Colormap.createDefTriangle();
            ColormapsManager.tryAcceptingTexture(textures, id, defaultColormap, usedTextures, true);

            addModifier(id, DimensionEffectsModifier.ofFogColor(defaultColormap), access);
        }
    }

    private void addModifier(ResourceLocation fileId, DimensionEffectsModifier mod, HolderLookup.Provider registryAccess) {
        for (var h : mod.targets().getTargets(fileId, registryAccess)) {
            effectsToApply.merge(h.unwrapKey().get().location(), mod, DimensionEffectsModifier::merge);
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider registryAccess, boolean isLogIn) {
        if (!isLogIn && !needsDynamicApplication) return;
        needsDynamicApplication = false;

        for (var v : vanillaEffects.entrySet()) {
            v.getValue().applyInplace(v.getKey());
        }

        var dimReg = registryAccess.lookupOrThrow(Registries.DIMENSION_TYPE);

        for (var v : effectsToApply.entrySet()) {
            ResourceLocation dimensionId = v.getKey();
            var dimensionKey = ResourceKey.create(Registries.DIMENSION_TYPE, dimensionId);
            DimensionEffectsModifier modifier = v.getValue();
            var old = modifier.applyInplace(dimensionId);

            vanillaEffects.put(dimensionId, old);

            DimensionType dim = dimReg.get(dimensionKey).get().value();
            if (modifier.getFogColormap() instanceof IColorGetter c) {
                fogColormaps.put(dim, c);
            }
            if (modifier.getTerrainFogColormap() instanceof IColorGetter c) {
                terrainFogColormaps.put(dim, c);
            }
            if (modifier.getSkyColormap() instanceof IColorGetter c) {
                skyColormaps.put(dim, c);
            }
            if (modifier.getSunsetColormap() instanceof IColorGetter c) {
                sunsetColormaps.put(dim, c);
            }
            if (modifier.noWeatherFogDarken()) {
                cancelFogWeatherDarken.put(dim, true);
            }
            if (modifier.noWeatherSkyDarken()) {
                cancelSkyWeatherDarken.put(dim, true);
            }
            if (modifier.cloudLevel().isPresent() && modifier.cloudLevel().get().right().isPresent()) {
                cloudFunctions.put(dim, modifier.cloudLevel().get().right().get());
            }
        }
        if (!vanillaEffects.isEmpty())
            Polytone.LOGGER.info("Applied {} Dimension Modifiers", vanillaEffects.size());
        //we don't clear effects to apply because we need to re apply on world reload

    }

    @Nullable
    public Vec3 modifyFogColor(Vec3 center, ClientLevel level, float brightness) {
        IColorGetter colormap = isTerrainHack.get() ? this.terrainFogColormaps.get(level.dimensionType()) :
                this.fogColormaps.get(level.dimensionType());
        if (colormap == null) return null;
        return cubicSample(center, level, brightness, colormap);
    }

    private static final ThreadLocal<Boolean> isTerrainHack = ThreadLocal.withInitial(() -> false);


    public Vector4f modifyTerrainFogColor(Vector4f original, ClientLevel level, Camera camera, float partialTicks, GameRenderer gameRenderer, Minecraft minecraft) {
        IColorGetter colormap = this.terrainFogColormaps.get(level.dimensionType());
        if (colormap == null) return original;
        isTerrainHack.set(true);
      //  Vector4f vector4f = FogRenderer.computeFogColor(
        //        camera, partialTicks, level, minecraft.options.getEffectiveRenderDistance(),
          //      gameRenderer.getDarkenWorldAmount(partialTicks), false
        //);
        isTerrainHack.set(false);
        return null;
    }


    private static @NotNull Vec3 cubicSample(Vec3 center, ClientLevel level, float brightness, IColorGetter colormap) {
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
        IColorGetter colormap = this.skyColormaps.get(level.dimensionType());
        if (colormap == null) return null;

        //same as original one
        BiomeManager biomeManager = level.getBiomeManager();
        return CubicSampler.gaussianSampleVec3(center, (qx, qy, qz) -> {
            var biome = biomeManager.getNoiseBiomeAtQuart(qx, qy, qz).value();
            //int skyColor = biome.getSkyColor();
            int skyColor1 = colormap.sampleColor(null, BlockPos.containing(qx * 4, qy * 4, qz * 4), biome, null); //quart coords to block coord
            return Vec3.fromRGB24(skyColor1);
        });
    }


    public Optional<Integer> modifyCloudHeight(ClientLevel level) {
        BlockContextExpression height = this.cloudFunctions.get(level.dimensionType());
        if (height == null) return Optional.empty();
        BlockPos pos = ClientFrameTicker.getCameraPos();
        double v = height.getValue(level, pos, Blocks.AIR.defaultBlockState());
        if (v >= 10000) {
            return  Optional.of(Integer.MAX_VALUE);
        }
        return Optional.of((int) v);
    }

    public boolean shouldCancelFogWeatherDarken(Level level) {
        return this.cancelFogWeatherDarken.getOrDefault(level.dimensionType(), false);
    }

    public boolean shouldCancelSkyWeatherDarken(Level level) {
        return this.cancelSkyWeatherDarken.getOrDefault(level.dimensionType(), false);
    }

    public void addConvertedBlockProperties(Map<ResourceLocation, Parsed<DimensionEffectsModifier>> converted) {
        extraMods.clear();
        extraMods.putAll(converted);
    }

    private static float[] lastSunset = null;

    @Nullable
    public Integer modifySunsetColor(int old) {
        IColorGetter colormap = this.sunsetColormaps.get(Minecraft.getInstance().level.dimensionType());
        if (colormap == null) return null;
        float oldAlpha = ARGB.alpha(old) / 255f;
        var color = colormap.sampleColor(null, ClientFrameTicker.getCameraPos(),
                ClientFrameTicker.getCameraBiome().value(), null);

        float deltaTime = ClientFrameTicker.getDeltaTime(); // Get time since last frame
        float interpolationFactor = deltaTime * 0.1f;


        var c = ColorUtils.unpack(color);

        if (lastSunset == null) {
            lastSunset = new float[]{c[0], c[1], c[2], oldAlpha};
            return ARGB.colorFromFloat(lastSunset[3], lastSunset[0], lastSunset[1], lastSunset[2]);
        }
        // Interpolate towards the fogScalars values
        lastSunset[0] = Mth.lerp(interpolationFactor, lastSunset[0], c[0]);
        lastSunset[1] = Mth.lerp(interpolationFactor, lastSunset[1], c[1]);
        lastSunset[2] = Mth.lerp(interpolationFactor, lastSunset[2], c[2]);
        lastSunset[3] = oldAlpha;
        return ARGB.colorFromFloat(lastSunset[3], lastSunset[0], lastSunset[1], lastSunset[2]);
    }


}
