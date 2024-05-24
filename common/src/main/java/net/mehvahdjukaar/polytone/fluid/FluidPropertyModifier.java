package net.mehvahdjukaar.polytone.fluid;

import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.block.BlockPropertyModifier;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.CompoundBlockColors;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.mehvahdjukaar.polytone.utils.TargetsHelper;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public record FluidPropertyModifier(Optional<BlockColor> colormap, Optional<BlockColor> fogColormap,
                                    Optional<Set<ResourceLocation>> explicitTargets) {

    public static final Decoder<FluidPropertyModifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    StrOpt.of(Colormap.CODEC, "colormap").forGetter(FluidPropertyModifier::colormap),
                    StrOpt.of(Colormap.CODEC, "fog_colormap").forGetter(FluidPropertyModifier::fogColormap),
                    StrOpt.of(TargetsHelper.CODEC, "targets").forGetter(FluidPropertyModifier::explicitTargets)
            ).apply(instance, FluidPropertyModifier::new));

    // Other has priority
    public FluidPropertyModifier merge(FluidPropertyModifier other) {
        return new FluidPropertyModifier(
                other.colormap.isPresent() ? other.colormap() : this.colormap(),
                other.fogColormap().isPresent() ? other.fogColormap() : this.fogColormap(),
                TargetsHelper.merge(other.explicitTargets, this.explicitTargets)
        );
    }

    public static FluidPropertyModifier ofBlockColor(BlockColor colormap) {
        return new FluidPropertyModifier(Optional.of(colormap), Optional.empty(), Optional.empty());
    }
    @Nullable
    public BlockColor getColormap() {
        return colormap.orElse(null);
    }

    @Nullable
    public BlockColor getFogColormap() {
        return fogColormap.orElse(null);
    }

}
