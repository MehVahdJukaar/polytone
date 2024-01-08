package net.mehvahdjukaar.polytone.properties;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.mehvahdjukaar.polytone.utils.input_source.ExpressionSource;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.mehvahdjukaar.polytone.properties.BlockPropertiesManager.COLORMAPS_IDS;

public class Colormap implements BlockColor {

    final Int2ObjectMap<ColormapTintGetter> getters = new Int2ObjectArrayMap<>();
    boolean isReference = false;

    protected static final Codec<Colormap> DIRECT_CODEC = Codec.simpleMap(Codec.STRING, ColormapTintGetter.SINGLE,
                    Keyable.forStrings(() -> IntStream.rangeClosed(-1, 16).mapToObj(String::valueOf)))
            .xmap(Colormap::new, Colormap::toStringMap).codec();


    protected static final Codec<Colormap> COLORMAP_REFERENCE_CODEC = ResourceLocation.CODEC.flatXmap(
            id -> Optional.ofNullable(COLORMAPS_IDS.get(id)).map(DataResult::success)
                    .orElse(DataResult.error(() -> "Unknown Color Property with id " + id)),
            object -> Optional.ofNullable(COLORMAPS_IDS.inverse().get(object)).map(DataResult::success)
                    .orElse(DataResult.error(() -> "Unknown Color Property: " + object)));

    public static final Codec<Colormap> CODEC =
            ExtraCodecs.validate(new ReferenceOrDirectCodec<>(COLORMAP_REFERENCE_CODEC, Colormap.DIRECT_CODEC,
                            i -> i.isReference = true),
                    j -> j.getters.size() == 0 ? DataResult.error(() -> "Must have at least 1 tint getter") :
                            DataResult.success(j));

    private Colormap(Map<String, ColormapTintGetter> map) {
        for (var e : map.entrySet()) {
            getters.put(Integer.parseInt(e.getKey()), e.getValue());
        }
    }

    public Int2ObjectMap<ColormapTintGetter> getGetters() {
        return getters;
    }

    public Map<String, ColormapTintGetter> toStringMap() {
        return getters.int2ObjectEntrySet().stream()
                .collect(Collectors.toMap(entry -> String.valueOf(entry.getIntKey()), Map.Entry::getValue));
    }

    @Override
    public int getColor(BlockState blockState, @Nullable BlockAndTintGetter level, @Nullable BlockPos blockPos, int tintIndex) {
        var getter = getters.get(tintIndex);
        if (getter != null) return getter.getColor(blockState, level, blockPos);
        else {
            getter = getters.get(-1);
            if (getter != null) return getter.getColor(blockState, level, blockPos);
        }
        return -1;
    }


    public static class ColormapTintGetter {

        private final int defaultColor;
        private final ExpressionSource xGetter;
        private final ExpressionSource yGetter;

        ArrayImage image;

        private static final Codec<ColormapTintGetter> SINGLE = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("default_color", -1).forGetter(c -> c.defaultColor),
                ExpressionSource.CODEC.fieldOf("x_axis").forGetter(c -> c.xGetter),
                ExpressionSource.CODEC.fieldOf("y_axis").forGetter(c -> c.yGetter)
        ).apply(i, ColormapTintGetter::new));

        private ColormapTintGetter(int defaultColor, ExpressionSource xGetter, ExpressionSource yGetter) {
            this.defaultColor = defaultColor;
            this.xGetter = xGetter;
            this.yGetter = yGetter;
        }

        public int getColor(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            float humidity = Mth.clamp(xGetter.getValue(state, level, pos), 0, 1);
            float temperature = Mth.clamp(yGetter.getValue(state, level, pos), 0, 1);
            humidity *= temperature;
            int i = (int) ((1.0 - temperature) * image.width());
            int j = (int) ((1.0 - humidity) * image.height());
            int k = j << 8 | i;
            int[] pixels = image.pixels();
            return k >= pixels.length ? defaultColor : pixels[k];
        }
    }



    public static final ColorResolver TEMPERATURE_RESOLVER = (biome, x, z) ->
            Float.floatToIntBits(biome.climateSettings.temperature);

    public static final ColorResolver DOWNFALL_RESOLVER = (biome, x, z) ->
            Float.floatToIntBits(biome.climateSettings.downfall);


}
