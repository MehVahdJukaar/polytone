package net.mehvahdjukaar.polytone.fluid;

import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.colormap.TintMap;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.client.color.block.BlockColor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record FluidPropertyModifier(Optional<BlockColor> colormap, Optional<BlockColor> fogColormap) {

    public static final Decoder<FluidPropertyModifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    StrOpt.of(TintMap.CODEC, "colormap").forGetter(FluidPropertyModifier::colormap),
                    StrOpt.of(TintMap.CODEC, "fog_colormap").forGetter(FluidPropertyModifier::fogColormap)
            ).apply(instance, FluidPropertyModifier::new));

    @Nullable
    public BlockColor getColormap() {
        return colormap.orElse(null);
    }

    @Nullable
    public BlockColor getFogColormap() {
        return fogColormap.orElse(null);
    }

}
