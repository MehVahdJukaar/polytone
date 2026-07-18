package net.mehvahdjukaar.polytone.content.dimension;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.companion.TexturePart;
import net.mehvahdjukaar.polytone.companion.TrackedTextures;
import net.mehvahdjukaar.polytone.content.block.BlockContextExpression;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DimensionEffectsManager extends JsonImgPartialReloader<DimensionEffectsModifier> {

    //we cant store dimensions here since the dimension registry isnt synced to the client
    //!!map of dimension modifier IDs to modifiers
    private final MapRegistry<DimensionEffectsModifier> dimensionEffects = new MapRegistry<>("Dimension Effects Modifiers");

    //map of dimension ID to modifier
    private final Map<ResourceLocation, DimensionEffectsModifier> alteredVanillaEffects = new HashMap<>();

    private final Object2ObjectMap<DimensionType, IColorGetter> fogColormaps = new Object2ObjectArrayMap<>();
    private final Object2ObjectMap<DimensionType, IColorGetter> terrainFogColormaps = new Object2ObjectArrayMap<>();
    private final Object2ObjectMap<DimensionType, IColorGetter> skyColormaps = new Object2ObjectArrayMap<>();
    private final Object2ObjectMap<DimensionType, IColorGetter> sunsetColormaps = new Object2ObjectArrayMap<>();
    private final Object2ObjectMap<DimensionType, BlockContextExpression> cloudFunctions = new Object2ObjectArrayMap<>();
    private final Object2BooleanArrayMap<DimensionType> cancelFogWeatherDarken = new Object2BooleanArrayMap<>();
    private final Object2BooleanArrayMap<DimensionType> cancelSkyWeatherDarken = new Object2BooleanArrayMap<>();

    private boolean needsDynamicApplication = true;

    private final Map<ResourceLocation, Parsed<DimensionEffectsModifier>> extraMods = new HashMap<>();

    // first part = main feature: a plain <name>.png reads as fog
    private static final TexturePart<DimensionEffectsModifier> FOG = TexturePart.suffix("_fog", DimensionEffectsModifier::getFogColormap);
    private static final TexturePart<DimensionEffectsModifier> SKY = TexturePart.suffix("_sky", DimensionEffectsModifier::getSkyColormap);
    private static final TexturePart<DimensionEffectsModifier> SUNSET = TexturePart.suffix("_sunset", DimensionEffectsModifier::getSunsetColormap);
    private static final TexturePart<DimensionEffectsModifier> TERRAIN_FOG = TexturePart.suffix("_terrain_fog", DimensionEffectsModifier::getTerrainFogColormap);

    public DimensionEffectsManager() {
        // 1.21.1 DimensionEffectsModifier.CODEC is only a Decoder, not a full Codec; not editable until ported.
        super(Spec.<DimensionEffectsModifier>of("Dimension modifier")
                .wikiPage("Dimension-Effects-Modifiers")
                .textureParts(FOG, SKY, SUNSET, TERRAIN_FOG)
                .folders("dimension_modifiers", "dimension_effects"));
    }

    private static DimensionEffectsModifier defaultFor(TexturePart<DimensionEffectsModifier> part) {
        if (part == SKY) return DimensionEffectsModifier.ofSkyColor(Colormap.createDefTriangle());
        if (part == SUNSET) return DimensionEffectsModifier.ofSunsetColor(Colormap.createTimeStrip());
        if (part == TERRAIN_FOG) return DimensionEffectsModifier.ofTerrainFogColor(Colormap.createDefTriangle());
        return DimensionEffectsModifier.ofFogColor(Colormap.createDefTriangle());
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        needsDynamicApplication = true;

        //Dimensions are NOT reloaded with world load. we need to reset vanilla stuff once we have a level
        //whatever happens, we always clear stuff to apply
        dimensionEffects.clear();
        fogColormaps.clear();
        skyColormaps.clear();
        sunsetColormaps.clear();
        cancelFogWeatherDarken.clear();
        cancelSkyWeatherDarken.clear();
        cloudFunctions.clear();
        extraMods.clear();
    }

    @Override
    protected void parseWithLevel(Resources resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
        var jsons = resources.jsons();
        var textures = new TrackedTextures(resources.textures());

        Parsed.SortedMap<DimensionEffectsModifier> parsedModifiers =
                Parsed.batchParseAlways(jsons, DimensionEffectsModifier.CODEC, ops, "dimension modifier");
        parsedModifiers.putAll(extraMods);

        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers) {
            ResourceLocation id = entry.getKey();
            Parsed<DimensionEffectsModifier> parsed = entry.getValue();
            DimensionEffectsModifier modifier = parsed.getResultOrPartial();

            // auto-attach a default colormap for every texture present with no colormap declared,
            // then fill inline colormaps from the scanned textures
            for (var part : contentTexture.adoptable(textures, id, modifier).keySet()) {
                modifier = modifier.merge(defaultFor(part));
            }
            contentTexture.fill(textures, id, modifier, true);

            if (parsed.isEnabled()) {
                addModifier(id, modifier, access);
            }
        }

        // creates orphaned texture colormaps & properties (legacy colormatic-style lone textures)
        for (var orphan : contentTexture.orphans(textures, parsedModifiers.keySet())) {
            DimensionEffectsModifier modifier = null;
            for (var part : orphan.parts().keySet()) {
                DimensionEffectsModifier d = defaultFor(part);
                modifier = modifier == null ? d : modifier.merge(d);
            }
            contentTexture.fill(textures, orphan.stemId(), modifier, true);
            addModifier(orphan.stemId(), modifier, access);
        }
    }

    private void addModifier(ResourceLocation fileId, DimensionEffectsModifier mod, RegistryAccess registryAccess) {
        dimensionEffects.register(fileId, mod);
    }

    @Override
    protected void applyWithLevel(RegistryAccess registryAccess, boolean isLogIn) {
        if (!isLogIn && !needsDynamicApplication) return;
        needsDynamicApplication = false;

        //reset vanilla
        for (var v : alteredVanillaEffects.entrySet()) {
            v.getValue().applyInplace(v.getKey());
        }

        //apply to current dimension
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            onDimensionChanged(level.dimensionTypeRegistration(), registryAccess);
        }
    }

    public void onDimensionChanged(Holder<DimensionType> currentDimHolder, RegistryAccess access) {
        DimensionType currentDim = currentDimHolder.value();
        ResourceLocation currentDimId = currentDimHolder.unwrapKey().get().location();

        for (var v : dimensionEffects.getEntries()) {
            ResourceLocation modId = v.getKey();
            DimensionEffectsModifier modifier = v.getValue();
            var targets = modifier.targets().compute(modId, access);
            if (!targets.contains(currentDimHolder)) continue;

            var old = modifier.applyInplace(currentDimId);

            alteredVanillaEffects.put(currentDimId, old);

            if (modifier.getFogColormap() instanceof IColorGetter c) {
                fogColormaps.put(currentDim, c);
            }
            if (modifier.getTerrainFogColormap() instanceof IColorGetter c) {
                terrainFogColormaps.put(currentDimHolder.value(), c);
            }
            if (modifier.getSkyColormap() instanceof IColorGetter c) {
                skyColormaps.put(currentDim, c);
            }
            if (modifier.getSunsetColormap() instanceof IColorGetter c) {
                sunsetColormaps.put(currentDim, c);
            }
            if (modifier.noWeatherFogDarken()) {
                cancelFogWeatherDarken.put(currentDim, true);
            }
            if (modifier.noWeatherSkyDarken()) {
                cancelSkyWeatherDarken.put(currentDim, true);
            }
            if (modifier.cloudLevel().isPresent() && modifier.cloudLevel().get().right().isPresent()) {
                cloudFunctions.put(currentDim, modifier.cloudLevel().get().right().get());
            }
            Polytone.LOGGER.info("Applied Custom Dimension Effects Modifier '{}' to dimension '{}'", modId, currentDimHolder);
        }
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


    /*
    public Vector4f modifyTerrainFogColor(Vector4f original, ClientLevel level, Camera camera, float partialTicks, GameRenderer gameRenderer, Minecraft minecraft) {
        IColorGetter colormap = this.terrainFogColormaps.get(level.dimensionType());
        if (colormap == null) return original;
        isTerrainHack.set(true);
        Vector4f vector4f = FogRenderer.computeFogColor(
                camera, partialTicks, level, minecraft.options.getEffectiveRenderDistance(),
                gameRenderer.getDarkenWorldAmount(partialTicks)
        );
        isTerrainHack.set(false);
        return vector4f;
    }*/ //TODO add


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

        BiomeManager biomeManager = level.getBiomeManager();
        return CubicSampler.gaussianSampleVec3(center, (qx, qy, qz) -> {
            var biome = biomeManager.getNoiseBiomeAtQuart(qx, qy, qz).value();
            //int skyColor = biome.getSkyColor();
            int skyColor1 = colormap.sampleColor(null, BlockPos.containing(qx * 4, qy * 4, qz * 4), biome, null); //quart coords to block coord
            return Vec3.fromRGB24(skyColor1);
        });
    }


    @Nullable
    public Float modifyCloudHeight(ClientLevel level) {
        BlockContextExpression height = this.cloudFunctions.get(level.dimensionType());
        if (height == null) return null;
        BlockPos pos = ClientFrameTicker.getCameraPos();
        double v = height.getValue(level, pos, Blocks.AIR.defaultBlockState());
        if (v >= 10000) {
            return Float.NaN;
        }
        return (float) v;
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

    public float @Nullable [] modifySunsetColor(float[] old) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;
        IColorGetter colormap = this.sunsetColormaps.get(level.dimensionType());
        if (colormap == null) return null;
        var color = colormap.sampleColor(null, ClientFrameTicker.getCameraPos(),
                ClientFrameTicker.getCameraBiome().value(), null);

        float deltaTime = ClientFrameTicker.getDeltaTime();
        float interpolationFactor = deltaTime * 0.1f;


        var c = ColorUtils.unpack(color);

        if (lastSunset == null) {
            lastSunset = new float[]{c[0], c[1], c[2], old[3]};
            return lastSunset;
        }
        // Interpolate towards the fogScalars values
        lastSunset[0] = Mth.lerp(interpolationFactor, lastSunset[0], c[0]);
        lastSunset[1] = Mth.lerp(interpolationFactor, lastSunset[1], c[1]);
        lastSunset[2] = Mth.lerp(interpolationFactor, lastSunset[2], c[2]);
        lastSunset[3] = old[3];
        return lastSunset;
    }


}
