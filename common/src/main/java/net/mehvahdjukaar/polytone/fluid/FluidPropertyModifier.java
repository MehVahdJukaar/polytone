package net.mehvahdjukaar.polytone.fluid;

import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.client.color.block.BlockColor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record FluidPropertyModifier(Optional<BlockColor> colormap) {

    public static final Decoder<FluidPropertyModifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    StrOpt.of(net.mehvahdjukaar.polytone.colormap.Colormap.CODEC, "colormap").forGetter(FluidPropertyModifier::colormap)
            ).apply(instance, FluidPropertyModifier::new));

    @Nullable
    public BlockColor getColormap() {
        return  colormap.orElse(null);
    }

}
