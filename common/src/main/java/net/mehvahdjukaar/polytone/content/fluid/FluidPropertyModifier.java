package net.mehvahdjukaar.polytone.content.fluid;

import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.common.Targets;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record FluidPropertyModifier(Optional<IColorGetter> colormap, Optional<IColorGetter> fogColormap,
                                    Targets targets) {

    public static final Decoder<FluidPropertyModifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Colormap.CODEC.optionalFieldOf("colormap").forGetter(FluidPropertyModifier::colormap),
                    Colormap.CODEC.optionalFieldOf("fog_colormap").forGetter(FluidPropertyModifier::fogColormap),
                    Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(FluidPropertyModifier::targets)
            ).apply(instance, FluidPropertyModifier::new));

    // Other has priority
    public FluidPropertyModifier merge(FluidPropertyModifier newMod) {
        return new FluidPropertyModifier(
                newMod.colormap.isPresent() ? newMod.colormap() : this.colormap(),
                newMod.fogColormap().isPresent() ? newMod.fogColormap() : this.fogColormap(),
                newMod.targets.merge(this.targets)
        );
    }

    public static FluidPropertyModifier ofBlockColor(IColorGetter colormap) {
        return new FluidPropertyModifier(Optional.of(colormap), Optional.empty(), Targets.EMPTY);
    }

    @Nullable
    public IColorGetter getColormap() {
        return colormap.orElse(null);
    }

    @Nullable
    public IColorGetter getFogColormap() {
        return fogColormap.orElse(null);
    }

    public boolean hasColormap() {
        return colormap.isPresent();
    }
}
