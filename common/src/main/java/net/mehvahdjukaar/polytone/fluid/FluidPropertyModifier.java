package net.mehvahdjukaar.polytone.fluid;

import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.biome.BiomeEffectModifier;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.utils.AlternativeMapCodec;
import net.mehvahdjukaar.polytone.utils.FogManager;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.client.color.block.BlockColor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record FluidPropertyModifier(Optional<? extends BlockColor> colormap, Optional<IColorGetter> fogColormap,
                                    Optional<FogManager.FogParam> fogFade,
                                    Optional<FogManager.FogParam> fogRadius,
                                    Targets targets) {

    public static final Decoder<FluidPropertyModifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    StrOpt.of(Colormap.CODEC, "colormap").forGetter(c -> (Optional<IColorGetter>) c.colormap),
                    StrOpt.of(Colormap.CODEC, "fog_colormap").forGetter(FluidPropertyModifier::fogColormap),
                    AlternativeMapCodec.optionalAlias( FogManager.FogParam.CODEC, "fog_fade", "fog_start").forGetter(FluidPropertyModifier::fogFade),
                    AlternativeMapCodec.optionalAlias(FogManager.FogParam.CODEC, "fog_radius", "fog_end").forGetter(FluidPropertyModifier::fogRadius),
                    Targets.CODEC.optionalFieldOf("targets", net.mehvahdjukaar.polytone.utils.Targets.EMPTY).forGetter(FluidPropertyModifier::targets)
            ).apply(instance, FluidPropertyModifier::new));

    // Other has priority
    public FluidPropertyModifier merge(FluidPropertyModifier newMod) {
        return new FluidPropertyModifier(
                newMod.colormap.isPresent() ? newMod.colormap() : this.colormap(),
                newMod.fogColormap().isPresent() ? newMod.fogColormap() : this.fogColormap(),
                newMod.fogFade().isPresent() ? newMod.fogFade() : this.fogFade(),
                newMod.fogRadius().isPresent() ? newMod.fogRadius() : this.fogRadius(),
                newMod.targets.merge(this.targets)
        );
    }

    public static FluidPropertyModifier ofBlockColor(BlockColor colormap) {
        return new FluidPropertyModifier(Optional.of(colormap), Optional.empty(),
                Optional.empty(), Optional.empty(), Targets.EMPTY);
    }

    @Nullable
    public BlockColor getColormap() {
        return colormap.orElse(null);
    }

    @Nullable
    public BlockColor getFogColormap() {
        return fogColormap.orElse(null);
    }

    public boolean hasColormap() {
        return colormap.isPresent();
    }
}
