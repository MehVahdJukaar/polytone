package net.mehvahdjukaar.polytone.fluid;

import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.client.color.block.BlockColor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public record FluidPropertyModifier(Optional<? extends BlockColor> colormap, Optional<IColorGetter> fogColormap,
                                    Targets targets) {

    public static final Decoder<FluidPropertyModifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    StrOpt.of(Colormap.CODEC, "colormap").forGetter(c -> (Optional<IColorGetter>) c.colormap),
                    StrOpt.of(Colormap.CODEC, "fog_colormap").forGetter(FluidPropertyModifier::fogColormap),
                    Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(FluidPropertyModifier::targets)
            ).apply(instance, FluidPropertyModifier::new));

    // Other has priority
    public FluidPropertyModifier merge(FluidPropertyModifier other) {
        return new FluidPropertyModifier(
                other.colormap.isPresent() ? other.colormap() : this.colormap(),
                other.fogColormap().isPresent() ? other.fogColormap() : this.fogColormap(),
                other.targets.merge(this.targets)
        );
    }

    public static FluidPropertyModifier ofBlockColor(BlockColor colormap) {
        return new FluidPropertyModifier(Optional.of(colormap), Optional.empty(), Targets.EMPTY);
    }

    @Nullable
    public BlockColor getTint() {
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
