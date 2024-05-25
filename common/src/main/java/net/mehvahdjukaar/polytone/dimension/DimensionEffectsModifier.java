package net.mehvahdjukaar.polytone.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.utils.ITargetProvider;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.mehvahdjukaar.polytone.utils.TargetsHelper;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CubicSampler;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public record DimensionEffectsModifier(Optional<Float> cloudLevel,
                                       Optional<Boolean> hasGround,
                                       Optional<DimensionSpecialEffects.SkyType> skyType,
                                       Optional<Boolean> forceBrightLightmap,
                                       Optional<Boolean> constantAmbientLight,
                                       Optional<BlockColor> fogColor,
                                       Optional<Set<ResourceLocation>> explicitTargets) implements ITargetProvider {

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
                    StrOpt.of(TargetsHelper.CODEC, "targets").forGetter(DimensionEffectsModifier::explicitTargets)
            ).apply(instance, DimensionEffectsModifier::new));

    public static DimensionEffectsModifier ofFogColor(Colormap colormap) {
        return new DimensionEffectsModifier(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.of(colormap), Optional.empty());
    }

    public DimensionEffectsModifier merge(DimensionEffectsModifier other) {
        return new DimensionEffectsModifier(
                other.cloudLevel.isPresent() ? other.cloudLevel : this.cloudLevel,
                other.hasGround.isPresent() ? other.hasGround : this.hasGround,
                other.skyType.isPresent() ? other.skyType : this.skyType,
                other.forceBrightLightmap.isPresent() ? other.forceBrightLightmap : this.forceBrightLightmap,
                other.constantAmbientLight.isPresent() ? other.constantAmbientLight : this.constantAmbientLight,
                other.fogColor.isPresent() ? other.fogColor : this.fogColor,
                TargetsHelper.merge(other.explicitTargets, this.explicitTargets)
        );
    }

    @Nullable
    public Vec3 computeFogColor(Vec3 center, ClientLevel level, int lightLevel) {
        if (this.fogColor.isEmpty()) return null;
        var colormap = this.fogColor.get();

        if (colormap instanceof Colormap c) {
            BiomeManager biomeManager = level.getBiomeManager();
            return level.effects().getBrightnessDependentFogColor(
                    CubicSampler.gaussianSampleVec3(center, (qx, qy, qz) -> {
                        var biome = biomeManager.getNoiseBiomeAtQuart(qx, qy, qz).value();
                        //int fogColor = biome.getFogColor();
                        int fogColor1 = c.sampleColor(null, BlockPos.containing(qx * 4, qy * 4, qz * 4), biome); //quark coords to block coord
                        return Vec3.fromRGB24(fogColor1);
                    }), lightLevel);
        }
        return null;
    }

    public boolean hasColormap() {
        return this.fogColor.isPresent();
    }

    public BlockColor getColormap() {
        return this.fogColor.orElse(null);
    }


    /*
    public void apply(DimensionSpecialEffects.Builder builder) {
        this.cloudLevel.ifPresent(builder::cloudLevel);
        this.hasGround.ifPresent(builder::hasGround);
        this.skyType.ifPresent(builder::skyType);
        this.forceBrightLightmap.ifPresent(builder::forceBrightLightmap);
        this.constantAmbientLight.ifPresent(builder::constantAmbientLight);
        this.fogColor.ifPresent(builder::fogColor);
    }*/

}