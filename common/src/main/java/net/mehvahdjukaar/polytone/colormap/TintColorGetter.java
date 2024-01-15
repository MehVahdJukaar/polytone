package net.mehvahdjukaar.polytone.colormap;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Keyable;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TintColorGetter implements BlockColor {

    //TODO: delegate to grass so we have quark compat
    public static final BlockColor GRASS_COLOR = (s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageGrassColor(l, p) : GrassColor.getDefaultColor();

    public static final BlockColor FOLIAGE_COLOR = (s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageFoliageColor(l, p) : FoliageColor.getDefaultColor();

    public static final BlockColor WATER_COLOR = (s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageWaterColor(l, p) : -1;

    public static final TintColorGetter BIOME_SAMPLE = new TintColorGetter(Map.of("-1", Colormap.def()));

    public static final TintColorGetter TR_BIOME_SAMPLE = new TintColorGetter(Map.of("-1", Colormap.defTriangle()));


    final Int2ObjectMap<Colormap> getters = new Int2ObjectArrayMap<>();
    boolean isReference = false;

    protected static final Codec<TintColorGetter> TINTMAP_DIRECT_CODEC = Codec.simpleMap(Codec.STRING, Colormap.CODEC,
                    Keyable.forStrings(() -> IntStream.rangeClosed(-1, 16).mapToObj(String::valueOf)))
            .xmap(TintColorGetter::new, TintColorGetter::toStringMap).codec();


    protected static final Codec<BlockColor> TINTMAP_REFERENCE_CODEC = ResourceLocation.CODEC.flatXmap(
            id -> Optional.ofNullable(Polytone.COLORMAPS.get(id)).map(DataResult::success)
                    .orElse(DataResult.error(() -> "Could not find a custom Colormap with id " + id +
                            " Did you place it in 'assets/[your pack]/polytone/colormaps/' ?")),
            object -> Optional.ofNullable(Polytone.COLORMAPS.getKey(object)).map(DataResult::success)
                    .orElse(DataResult.error(() -> "Unknown Color Property: " + object)));

    public static final Codec<BlockColor> TINTMAP_CODEC =
            ExtraCodecs.validate(new ReferenceOrDirectCodec<>(
                            TINTMAP_REFERENCE_CODEC, TintColorGetter.TINTMAP_DIRECT_CODEC, i ->
                    {
                        if (i instanceof TintColorGetter c) {
                            c.isReference = true;
                        }
                    }),
                    j -> {
                        if (j instanceof TintColorGetter c && c.getters.size() == 0) {
                            return DataResult.error(() -> "Must have at least 1 tint getter");
                        }
                        return DataResult.success(j);
                    });

    // single or multiple
    public static final Codec<BlockColor> CODEC = Codec.either(TINTMAP_CODEC, Colormap.CODEC)
            .xmap(either -> either.map(Function.identity(), TintColorGetter::new), Either::left);

    private TintColorGetter(Map<String, Colormap> map) {
        for (var e : map.entrySet()) {
            getters.put(Integer.parseInt(e.getKey()), e.getValue());
        }
    }

    private TintColorGetter() {
    }

    private TintColorGetter(Colormap colormap) {
        getters.put(-1, colormap);
    }

    // square simple map with all same tint
    public static TintColorGetter createSimple(IColormapNumberProvider xGetter, IColormapNumberProvider yGetter) {
        return new TintColorGetter(new Colormap(xGetter, yGetter));
    }

    // default biome sample vanilla implementation
    public static TintColorGetter createDefault(Set<Integer> tintIndexes, boolean triangular) {
        var c = new TintColorGetter();
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






}
