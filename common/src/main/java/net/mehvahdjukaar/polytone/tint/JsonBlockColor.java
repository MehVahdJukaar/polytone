package net.mehvahdjukaar.polytone.tint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.tint.input_source.InputSource;
import net.mehvahdjukaar.polytone.tint.input_source.InputSources;
import net.mehvahdjukaar.polytone.tint.input_source.JavaxExpression;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record JsonBlockColor(Int2ObjectMap<ColormapTintGetter> getters) implements BlockColor {

    public static final Codec<JsonBlockColor> CODEC = Codec.simpleMap(Codec.STRING, ColormapTintGetter.SINGLE,
                    Keyable.forStrings(() -> IntStream.rangeClosed(0, 16).mapToObj(String::valueOf)))
            .xmap(JsonBlockColor::new, JsonBlockColor::toStringMap).codec();

    private JsonBlockColor(Map<String, ColormapTintGetter> map) {
        this(new Int2ObjectArrayMap<>(map.entrySet().stream()
                .collect(Collectors.toMap(entry -> Integer.parseInt(entry.getKey()), Map.Entry::getValue))));
    }

    public Map<String, ColormapTintGetter> toStringMap(){
       return getters.int2ObjectEntrySet().stream()
                .collect(Collectors.toMap(entry -> String.valueOf(entry.getIntKey()), Map.Entry::getValue));
    }

    @Override
    public int getColor(BlockState blockState, @Nullable BlockAndTintGetter level, @Nullable BlockPos blockPos, int tintIndex) {
        var getter = getters.get(tintIndex);
        if (getter != null) return getter.getColor(blockState, level, blockPos);
        return -1;
    }


private record ColormapTintGetter(ResourceLocation colormapLoc, int defaultColor,
                                  JavaxExpression xGetter, JavaxExpression yGetter) {

    private static final Codec<ColormapTintGetter> SINGLE = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("colormap").forGetter(ColormapTintGetter::colormapLoc),
            Codec.INT.optionalFieldOf("default_color", -1).forGetter(ColormapTintGetter::defaultColor),
            JavaxExpression.CODEC.fieldOf("x_axis").forGetter(ColormapTintGetter::xGetter),
            JavaxExpression.CODEC.fieldOf("y_axis").forGetter(ColormapTintGetter::yGetter)
    ).apply(i, ColormapTintGetter::new));

    public int getColor(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        int[] pixels = ColormapsManager.getPixels(colormapLoc);
        if (pixels.length == 0) {
            Polytone.LOGGER.error("Missing colormapLoc texture at location " + colormapLoc);
            return -1;
        }
        /**
         float humidity = Mth.clamp(xGetter.getValue(state, level, pos), 0, 1);
         float temperature = Mth.clamp(yGetter.getValue(state, level, pos), 0, 1);
         humidity *= temperature;
         int i = (int) ((1.0 - temperature) * 255.0);
         int j = (int) ((1.0 - humidity) * 255.0);
         int k = j << 8 | i;
         return k >= pixels.length ? defaultColor : pixels[k];*/
        return -1;

    }
}


}
