package net.mehvahdjukaar.polytone.colormap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Colormap implements BlockColor {

    //TODO: delegate to grass so we have quark compat
    public static final BlockColor GRASS_COLOR = (s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageGrassColor(l, p) : GrassColor.getDefaultColor();

    public static final BlockColor FOLIAGE_COLOR = (s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageFoliageColor(l, p) : FoliageColor.getDefaultColor();

    public static final BlockColor WATER_COLOR = (s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageWaterColor(l, p) : -1;

    public static final Colormap BIOME_SAMPLE = new Colormap(Map.of("-1",
            new Sampler(Optional.of(-1), ExpressionSource.make("TEMPERATURE"),
                    ExpressionSource.make("DOWNFALL"), false)));

    public static final Colormap TR_BIOME_SAMPLE = new Colormap(Map.of("-1",
            new Sampler(Optional.of(-1), ExpressionSource.make("TEMPERATURE"),
                    ExpressionSource.make("DOWNFALL"), true)));


    final Int2ObjectMap<Sampler> getters = new Int2ObjectArrayMap<>();
    boolean isReference = false;

    protected static final Codec<Colormap> DIRECT_CODEC = Codec.simpleMap(Codec.STRING, Sampler.SINGLE,
                    Keyable.forStrings(() -> IntStream.rangeClosed(-1, 16).mapToObj(String::valueOf)))
            .xmap(Colormap::new, Colormap::toStringMap).codec();


    protected static final Codec<BlockColor> COLORMAP_REFERENCE_CODEC = ResourceLocation.CODEC.flatXmap(
            id -> Optional.ofNullable(ColormapsManager.get(id)).map(DataResult::success)
                    .orElse(DataResult.error(() -> "Could not find a custom Colormap with id " + id +
                            " Did you place it in 'assets/[your pack]/polytone/colormaps/' ?")),
            object -> Optional.ofNullable(ColormapsManager.getKey(object)).map(DataResult::success)
                    .orElse(DataResult.error(() -> "Unknown Color Property: " + object)));

    public static final Codec<BlockColor> CODEC =
            ExtraCodecs.validate(new ReferenceOrDirectCodec<>(
                            COLORMAP_REFERENCE_CODEC, (Codec<BlockColor>) (Object) Colormap.DIRECT_CODEC, i ->
                    {
                        if (i instanceof Colormap c) {
                            c.isReference = true;
                        }
                    }),
                    j -> {
                        if (j instanceof Colormap c && c.getters.size() == 0) {
                            return DataResult.error(() -> "Must have at least 1 tint getter");
                        }
                        return DataResult.success(j);
                    });

    private Colormap(Map<String, Sampler> map) {
        for (var e : map.entrySet()) {
            getters.put(Integer.parseInt(e.getKey()), e.getValue());
        }
    }

    private Colormap() {
    }

    // default biome sample vanilla implementation
    public static Colormap createDefault(Set<Integer> tintIndexes) {
        var c = new Colormap();
        for (var i : tintIndexes) {
            c.getters.put(i.intValue(), new Sampler(Optional.empty(),
                    ExpressionSource.make("TEMPERATURE"),
                    ExpressionSource.make("DOWNFALL"), false));
        }
        return c;
    }


    public Int2ObjectMap<Sampler> getGetters() {
        return getters;
    }

    public Map<String, Sampler> toStringMap() {
        return getters.int2ObjectEntrySet().stream()
                .collect(Collectors.toMap(entry -> String.valueOf(entry.getIntKey()), Map.Entry::getValue));
    }

    @Override
    public int getColor(BlockState blockState, @Nullable BlockAndTintGetter level, @Nullable BlockPos blockPos, int tintIndex) {
        Sampler getter = getters.get(tintIndex);
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


    public static class Sampler {

        private final ExpressionSource xGetter;
        private final ExpressionSource yGetter;
        private final boolean triangular;

        private Integer defaultColor = null;
        private ArrayImage image = null;

        private static final Codec<Sampler> SINGLE = RecordCodecBuilder.create(i -> i.group(
                StrOpt.of(Codec.INT, "default_color").forGetter(c -> Optional.ofNullable(c.defaultColor)),
                ExpressionSource.CODEC.fieldOf("x_axis").forGetter(c -> c.xGetter),
                ExpressionSource.CODEC.fieldOf("y_axis").forGetter(c -> c.yGetter),
                StrOpt.of(Codec.BOOL, "triangular", false).forGetter(c -> c.triangular)
        ).apply(i, Sampler::new));

        private Sampler(Optional<Integer> defaultColor, ExpressionSource xGetter, ExpressionSource yGetter, boolean triangular) {
            this.defaultColor = defaultColor.orElse(null);
            this.xGetter = xGetter;
            this.yGetter = yGetter;
            this.triangular = triangular;
        }

        public void acceptTexture(ArrayImage image) {
            this.image = image;
            if (defaultColor == null) {
                this.defaultColor = sample(0.5f, 0.5f, -1);
            }
        }

        public int getColor(@Nullable BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
            if (pos == null || level == null || state == null || image == null) return defaultColor;

            float humidity = Mth.clamp(xGetter.getValue(state, level, pos), 0, 1);
            float temperature = Mth.clamp(yGetter.getValue(state, level, pos), 0, 1);
            return sample(humidity, temperature, defaultColor);
        }

        private int sample(float x, float y, int defValue) {
            if (triangular) x *= y;
            int width = image.width();
            int height = image.height();

            int w = (int) ((1.0 - y) * (width-1));
            int h = (int) ((1.0 - x) * (height-1));

            return w >= width || h >= height ? defValue : image.pixels()[h][w];
        }
    }


    public static final ColorResolver TEMPERATURE_RESOLVER = (biome, x, z) ->
            Float.floatToIntBits(biome.climateSettings.temperature);

    public static final ColorResolver DOWNFALL_RESOLVER = (biome, x, z) ->
            Float.floatToIntBits(biome.climateSettings.downfall);


}
