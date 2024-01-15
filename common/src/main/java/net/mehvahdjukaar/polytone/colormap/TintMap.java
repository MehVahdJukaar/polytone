package net.mehvahdjukaar.polytone.colormap;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TintMap implements BlockColor {

    //TODO: delegate to grass so we have quark compat
    public static final BlockColor GRASS_COLOR = (s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageGrassColor(l, p) : GrassColor.getDefaultColor();

    public static final BlockColor FOLIAGE_COLOR = (s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageFoliageColor(l, p) : FoliageColor.getDefaultColor();

    public static final BlockColor WATER_COLOR = (s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageWaterColor(l, p) : -1;

    public static final TintMap BIOME_SAMPLE = new TintMap(Map.of("-1", Colormap.def()));

    public static final TintMap TR_BIOME_SAMPLE = new TintMap(Map.of("-1", Colormap.defTriangle()));


    final Int2ObjectMap<Colormap> getters = new Int2ObjectArrayMap<>();
    boolean isReference = false;

    protected static final Codec<TintMap> TINTMAP_DIRECT_CODEC = Codec.simpleMap(Codec.STRING, Colormap.CODEC,
                    Keyable.forStrings(() -> IntStream.rangeClosed(-1, 16).mapToObj(String::valueOf)))
            .xmap(TintMap::new, TintMap::toStringMap).codec();


    protected static final Codec<BlockColor> TINTMAP_REFERENCE_CODEC = ResourceLocation.CODEC.flatXmap(
            id -> Optional.ofNullable(Polytone.COLORMAPS.get(id)).map(DataResult::success)
                    .orElse(DataResult.error(() -> "Could not find a custom Colormap with id " + id +
                            " Did you place it in 'assets/[your pack]/polytone/colormaps/' ?")),
            object -> Optional.ofNullable(Polytone.COLORMAPS.getKey(object)).map(DataResult::success)
                    .orElse(DataResult.error(() -> "Unknown Color Property: " + object)));

    public static final Codec<BlockColor> TINTMAP_CODEC =
            ExtraCodecs.validate(new ReferenceOrDirectCodec<>(
                            TINTMAP_REFERENCE_CODEC, TintMap.TINTMAP_DIRECT_CODEC, i ->
                    {
                        if (i instanceof TintMap c) {
                            c.isReference = true;
                        }
                    }),
                    j -> {
                        if (j instanceof TintMap c && c.getters.size() == 0) {
                            return DataResult.error(() -> "Must have at least 1 tint getter");
                        }
                        return DataResult.success(j);
                    });

    // single or multiple
    public static final Codec<BlockColor> CODEC = Codec.either(TINTMAP_CODEC, Colormap.CODEC)
            .xmap(either -> either.map(Function.identity(), TintMap::new), Either::left);

    private TintMap(Map<String, Colormap> map) {
        for (var e : map.entrySet()) {
            getters.put(Integer.parseInt(e.getKey()), e.getValue());
        }
    }

    private TintMap() {
    }

    private TintMap(Colormap colormap) {
        getters.put(-1, colormap);
    }

    public static TintMap createSimple(IColormapNumberProvider xGetter, IColormapNumberProvider yGetter) {
        return new TintMap(new Colormap(xGetter, yGetter));
    }

    // default biome sample vanilla implementation
    public static TintMap createDefault(Set<Integer> tintIndexes, boolean triangular) {
        var c = new TintMap();
        for (var i : tintIndexes) {
            c.getters.put(i.intValue(), triangular ? Colormap.defTriangle() : Colormap.def());
        }
        return c;
    }


    public Int2ObjectMap<Colormap> getGetters() {
        return getters;
    }

    public Map<String, Colormap> toStringMap() {
        return getters.int2ObjectEntrySet().stream()
                .collect(Collectors.toMap(entry -> String.valueOf(entry.getIntKey()), Map.Entry::getValue));
    }

    @Override
    public int getColor(BlockState blockState, @Nullable BlockAndTintGetter level, @Nullable BlockPos blockPos, int tintIndex) {
        Colormap getter = getters.get(tintIndex);
        if (getter == null) {
            getter = getters.get(-1);
        }
        if (getter != null) {
            return getter.getColor(blockState, level, blockPos);
        }

        return -1;
    }

    public boolean isReference() {
        return isReference;
    }


    public static class Colormap {

        private final IColormapNumberProvider xGetter;
        private final IColormapNumberProvider yGetter;
        private final boolean triangular;

        private Integer defaultColor = null;
        private ArrayImage image = null;

        private static final Codec<Colormap> CODEC = RecordCodecBuilder.create(i -> i.group(
                StrOpt.of(Codec.INT, "default_color").forGetter(c -> Optional.ofNullable(c.defaultColor)),
                IColormapNumberProvider.CODEC.fieldOf("x_axis").forGetter(c -> c.xGetter),
                IColormapNumberProvider.CODEC.fieldOf("y_axis").forGetter(c -> c.yGetter),
                StrOpt.of(Codec.BOOL, "triangular", false).forGetter(c -> c.triangular)
        ).apply(i, Colormap::new));

        private Colormap(Optional<Integer> defaultColor, IColormapNumberProvider xGetter, IColormapNumberProvider yGetter, boolean triangular) {
            this.defaultColor = defaultColor.orElse(null);
            this.xGetter = xGetter;
            this.yGetter = yGetter;
            this.triangular = triangular;
        }

        private Colormap(IColormapNumberProvider xGetter, IColormapNumberProvider yGetter) {
            this(Optional.empty(), xGetter, yGetter, false);
        }

        public static Colormap def() {
            return new Colormap(Optional.of(-1),
                    IColormapNumberProvider.TEMPERATURE, IColormapNumberProvider.DOWNFALL, false);
        }

        public static Colormap defTriangle() {
            return new Colormap(Optional.of(-1),
                    IColormapNumberProvider.TEMPERATURE, IColormapNumberProvider.DOWNFALL, true);
        }

        public void acceptTexture(ArrayImage image) {
            this.image = image;
            if (defaultColor == null) {
                this.defaultColor = sample(0.5f, 0.5f, -1);
            }
        }

        public int getColor(@Nullable BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
            if (pos == null || level == null || state == null || image == null) {
                return defaultColor;
            }

            float humidity = Mth.clamp(xGetter.getValue(state, level, pos), 0, 1);
            float temperature = Mth.clamp(yGetter.getValue(state, level, pos), 0, 1);
            return sample(humidity, temperature, defaultColor);
        }

        private int sample(float x, float y, int defValue) {
            //if (Polytone.sodiumOn) return defValue;
            if (triangular) x *= y;
            int width = image.width();
            int height = image.height();

            int w = (int) ((1.0 - y) * (width - 1));
            int h = (int) ((1.0 - x) * (height - 1));

            return w >= width || h >= height ? defValue : image.pixels()[h][w];
        }
    }

    private static final int FLOAT_MULT = 100000000;

    public static float temperature(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        int t = level.getBlockTint(pos, TEMPERATURE_RESOLVER) & 255;
        return t / 255f;
    }


    public static float downfall(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        int t = level.getBlockTint(pos, DOWNFALL_RESOLVER) & 255;
        return t / 255f;
    }

    public static final ColorResolver TEMPERATURE_RESOLVER = (biome, x, z) -> {
        byte hack = (byte) (Mth.clamp(biome.climateSettings.temperature, 0, 1) * 255);
        return ColorUtils.pack(hack, hack, hack, hack);
    };

    public static final ColorResolver DOWNFALL_RESOLVER = (biome, x, z) -> {
        byte hack = (byte) (Mth.clamp(biome.climateSettings.downfall, 0, 1) * 255);
        return ColorUtils.pack(hack, hack, hack, hack);
    };

}
