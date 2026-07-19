package net.mehvahdjukaar.polytone.content.dimension;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.block.BlockContextExpression;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.content.lightmap.Lightmap;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


public record DimensionEffectsModifier(Optional<Either<Float, BlockContextExpression>> cloudLevel,
                                       Optional<Boolean> hasGround,
                                       Optional<DimensionSpecialEffects.SkyType> skyType,
                                       Optional<Boolean> forceBrightLightmap,
                                       Optional<Boolean> constantAmbientLight,
                                       Optional<IColorGetter> fogColor,
                                       Optional<IColorGetter> terrainFogColor,
                                       Optional<IColorGetter> skyColor,
                                       Optional<IColorGetter> sunsetColor,
                                       boolean noWeatherFogDarken,
                                       boolean noWeatherSkyDarken,
                                       Optional<Lightmap> lightmap, //TODO: finish adding
                                       DimensionTarget targets) {

    public static final Codec<DimensionSpecialEffects.SkyType> SKY_TYPE_CODEC = Codec.STRING
            .xmap(DimensionSpecialEffects.SkyType::valueOf, DimensionSpecialEffects.SkyType::name);

    public static final Codec<DimensionEffectsModifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.either(Codec.FLOAT, BlockContextExpression.CODEC).optionalFieldOf("cloud_level").forGetter(DimensionEffectsModifier::cloudLevel),
                    Codec.BOOL.optionalFieldOf("has_ground").forGetter(DimensionEffectsModifier::hasGround),
                    SKY_TYPE_CODEC.optionalFieldOf("sky_type").forGetter(DimensionEffectsModifier::skyType),
                    Codec.BOOL.optionalFieldOf("force_bright_lightmap").forGetter(DimensionEffectsModifier::forceBrightLightmap),
                    Codec.BOOL.optionalFieldOf("constant_ambient_light").forGetter(DimensionEffectsModifier::constantAmbientLight),
                    Colormap.CODEC.optionalFieldOf("fog_colormap").forGetter(DimensionEffectsModifier::fogColor),
                    Colormap.CODEC.optionalFieldOf("terrain_fog_colormap").forGetter(DimensionEffectsModifier::terrainFogColor),
                    Colormap.CODEC.optionalFieldOf("sky_colormap").forGetter(DimensionEffectsModifier::skyColor),
                    Colormap.CODEC.optionalFieldOf("sunset_colormap").forGetter(DimensionEffectsModifier::sunsetColor),
                    Codec.BOOL.optionalFieldOf("no_weather_fog_darken", false).forGetter(DimensionEffectsModifier::noWeatherFogDarken),
                    Codec.BOOL.optionalFieldOf("no_weather_sky_darken", false).forGetter(DimensionEffectsModifier::noWeatherSkyDarken),
                    Polytone.LIGHTMAPS.byNameCodec().optionalFieldOf("lightmap").forGetter(DimensionEffectsModifier::lightmap),
                    DimensionTarget.CODEC.optionalFieldOf("targets", DimensionTarget.EMPTY).forGetter(DimensionEffectsModifier::targets)
            ).apply(instance, DimensionEffectsModifier::new));

    public static DimensionEffectsModifier ofFogColor(Colormap colormap) {
        return new DimensionEffectsModifier(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.of(colormap), Optional.empty(), Optional.empty(), Optional.empty(),
                false, false, Optional.empty(), DimensionTarget.EMPTY);
    }

    public static DimensionEffectsModifier ofTerrainFogColor(Colormap colormap) {
        return new DimensionEffectsModifier(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.of(colormap), Optional.empty(), Optional.empty(),
                false, false, Optional.empty(), DimensionTarget.EMPTY);
    }

    public static DimensionEffectsModifier ofSkyColor(Colormap colormap) {
        return new DimensionEffectsModifier(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(colormap), Optional.empty(),
                false, false, Optional.empty(), DimensionTarget.EMPTY);
    }

    public static DimensionEffectsModifier ofSunsetColor(Colormap colormap) {
        return new DimensionEffectsModifier(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(),  Optional.empty(), Optional.empty(), Optional.of(colormap),
                false, false, Optional.empty(), DimensionTarget.EMPTY);
    }


    public DimensionEffectsModifier merge(DimensionEffectsModifier newMod) {
        return new DimensionEffectsModifier(
                newMod.cloudLevel.isPresent() ? newMod.cloudLevel : this.cloudLevel,
                newMod.hasGround.isPresent() ? newMod.hasGround : this.hasGround,
                newMod.skyType.isPresent() ? newMod.skyType : this.skyType,
                newMod.forceBrightLightmap.isPresent() ? newMod.forceBrightLightmap : this.forceBrightLightmap,
                newMod.constantAmbientLight.isPresent() ? newMod.constantAmbientLight : this.constantAmbientLight,
                newMod.fogColor.isPresent() ? newMod.fogColor : this.fogColor,
                newMod.terrainFogColor.isPresent() ? newMod.terrainFogColor : this.terrainFogColor,
                newMod.skyColor.isPresent() ? newMod.skyColor : this.skyColor,
                newMod.sunsetColor.isPresent() ? newMod.sunsetColor : this.sunsetColor,
                newMod.noWeatherFogDarken | this.noWeatherFogDarken,
                newMod.noWeatherSkyDarken | this.noWeatherSkyDarken,
                newMod.lightmap.isPresent() ? newMod.lightmap : this.lightmap,
                newMod.targets //ignore, not used after merging
        );
    }

    @Nullable
    public BlockColor getFogColormap() {
        return this.fogColor.orElse(null);
    }
    @Nullable
    public BlockColor getTerrainFogColormap() {
        return this.terrainFogColor.orElse(null);
    }
    
    @Nullable
    public BlockColor getSkyColormap() {
        return this.skyColor.orElse(null);
    }

    @Nullable
    public BlockColor getSunsetColormap() {
        return this.sunsetColor.orElse(null);
    }

    public DimensionEffectsModifier applyInplace(ResourceLocation dimensionId) {
        DimensionSpecialEffects effects = PlatStuff.getDimensionEffects(dimensionId);
        Optional<Either<Float, BlockContextExpression>> oldCloud = Optional.empty();
        if (this.cloudLevel.isPresent() && this.cloudLevel.get().left().isPresent()) {
            oldCloud = Optional.of(Either.left(effects.cloudLevel));
            effects.cloudLevel = this.cloudLevel.get().left().get();
        }
        Optional<Boolean> oldGround = Optional.empty();
        if (this.hasGround.isPresent()) {
            oldGround = Optional.of(effects.hasGround);
            effects.hasGround = this.hasGround.get();
        }
        Optional<DimensionSpecialEffects.SkyType> oldSky = Optional.empty();
        if (this.skyType.isPresent()) {
            oldSky = Optional.of(effects.skyType);
            effects.skyType = this.skyType.get();
        }
        Optional<Boolean> oldBright = Optional.empty();
        if (this.forceBrightLightmap.isPresent()) {
            oldBright = Optional.of(effects.forceBrightLightmap);
            effects.forceBrightLightmap = this.forceBrightLightmap.get();
        }
        Optional<Boolean> oldAmbient = Optional.empty();
        if (this.constantAmbientLight.isPresent()) {
            oldAmbient = Optional.of(effects.constantAmbientLight);
            effects.constantAmbientLight = this.constantAmbientLight.get();
        }
        return new DimensionEffectsModifier(oldCloud, oldGround, oldSky, oldBright, oldAmbient,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                false, false, Optional.empty(), DimensionTarget.EMPTY);
    }

}