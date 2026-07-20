package net.mehvahdjukaar.polytone.content.fluid;

import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.common.Targets;
import net.minecraft.client.color.block.BlockColor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record FluidPropertyModifier(Optional<? extends BlockColor> colormap, Optional<IColorGetter> fogColormap,
                                    Targets targets) {

    public static final SchemaCodec<FluidPropertyModifier> CODEC = SchemaRecord.create(FluidPropertyModifier.class, i ->
            i.group(
                    i.optional("colormap", Colormap.CODEC, c -> (Optional<IColorGetter>) c.colormap),
                    i.optional("fog_colormap", Colormap.CODEC, FluidPropertyModifier::fogColormap),
                    i.optional("targets", Targets.CODEC, Targets.EMPTY, FluidPropertyModifier::targets)
            ).apply(i, FluidPropertyModifier::new));

    // Other has priority
    public FluidPropertyModifier merge(FluidPropertyModifier newMod) {
        return new FluidPropertyModifier(
                newMod.colormap.isPresent() ? newMod.colormap() : this.colormap(),
                newMod.fogColormap().isPresent() ? newMod.fogColormap() : this.fogColormap(),
                newMod.targets.merge(this.targets)
        );
    }

    public static FluidPropertyModifier ofBlockColor(BlockColor colormap) {
        return new FluidPropertyModifier(Optional.of(colormap), Optional.empty(), Targets.EMPTY);
    }

    public static FluidPropertyModifier ofFogColor(IColorGetter colormap) {
        return new FluidPropertyModifier(Optional.empty(), Optional.of(colormap), Targets.EMPTY);
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
