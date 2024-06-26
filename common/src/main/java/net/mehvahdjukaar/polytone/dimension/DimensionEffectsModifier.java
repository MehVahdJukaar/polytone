package net.mehvahdjukaar.polytone.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.lightmap.Lightmap;
import net.mehvahdjukaar.polytone.utils.ITargetProvider;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

public record DimensionEffectsModifier(Optional<Float> cloudLevel,
                                       Optional<Boolean> hasGround,
                                       Optional<DimensionSpecialEffects.SkyType> skyType,
                                       Optional<Boolean> forceBrightLightmap,
                                       Optional<Boolean> constantAmbientLight,
                                       Optional<IColorGetter> fogColor,
                                       Optional<IColorGetter> skyColor,
                                       Optional<Lightmap> lightmap,
                                       Set<ResourceLocation> explicitTargets) implements ITargetProvider {

    public static final Codec<DimensionSpecialEffects.SkyType> SKY_TYPE_CODEC = Codec.STRING
            .xmap(DimensionSpecialEffects.SkyType::valueOf, DimensionSpecialEffects.SkyType::name);

    public static final Decoder<DimensionEffectsModifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    StrOpt.of(Codec.FLOAT, "cloud_level").forGetter(DimensionEffectsModifier::cloudLevel),
                    StrOpt.of(Codec.BOOL, "has_ground").forGetter(DimensionEffectsModifier::hasGround),
                    StrOpt.of(SKY_TYPE_CODEC, "sky_type").forGetter(DimensionEffectsModifier::skyType),
                    StrOpt.of(Codec.BOOL, "force_bright_lightmap").forGetter(DimensionEffectsModifier::forceBrightLightmap),
                    StrOpt.of(Codec.BOOL, "constant_ambient_light").forGetter(DimensionEffectsModifier::constantAmbientLight),
                    StrOpt.of(Colormap.CODEC, "fog_colormap").forGetter(DimensionEffectsModifier::fogColor),
                    StrOpt.of(Colormap.CODEC, "sky_colormap").forGetter(DimensionEffectsModifier::skyColor),
                    StrOpt.of(Polytone.LIGHTMAPS.byNameCodec(), "lightmap").forGetter(DimensionEffectsModifier::lightmap), //Just references for now
                    StrOpt.of(TARGET_CODEC, "targets", Set.of()).forGetter(DimensionEffectsModifier::explicitTargets)
            ).apply(instance, DimensionEffectsModifier::new));

    public static DimensionEffectsModifier ofFogColor(Colormap colormap) {
        return new DimensionEffectsModifier(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.of(colormap), Optional.empty(), Optional.empty(), Set.of());
    }

    public static DimensionEffectsModifier ofSkyColor(Colormap colormap) {
        return new DimensionEffectsModifier(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.of(colormap), Optional.empty(), Set.of());
    }


    public DimensionEffectsModifier merge(DimensionEffectsModifier other) {
        return new DimensionEffectsModifier(
                other.cloudLevel.isPresent() ? other.cloudLevel : this.cloudLevel,
                other.hasGround.isPresent() ? other.hasGround : this.hasGround,
                other.skyType.isPresent() ? other.skyType : this.skyType,
                other.forceBrightLightmap.isPresent() ? other.forceBrightLightmap : this.forceBrightLightmap,
                other.constantAmbientLight.isPresent() ? other.constantAmbientLight : this.constantAmbientLight,
                other.fogColor.isPresent() ? other.fogColor : this.fogColor,
                other.skyColor.isPresent() ? other.skyColor : this.skyColor,
                other.lightmap.isPresent() ? other.lightmap : this.lightmap,
                mergeSet(other.explicitTargets, this.explicitTargets)
        );
    }

    public BlockColor getFogColormap() {
        return this.fogColor.orElse(null);
    }

    public BlockColor getSkyColormap() {
        return this.skyColor.orElse(null);
    }

    public DimensionEffectsModifier applyInplace(ResourceLocation dimensionId) {
        DimensionSpecialEffects effects = PlatStuff.getDimensionEffects(dimensionId);
        Optional<Float> oldCloud = Optional.empty();
        if (this.cloudLevel.isPresent()) {
            oldCloud = Optional.of(effects.cloudLevel);
            effects.cloudLevel = this.cloudLevel.get();
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
                Optional.empty(), Optional.empty(), Optional.empty(), Set.of());
    }

}