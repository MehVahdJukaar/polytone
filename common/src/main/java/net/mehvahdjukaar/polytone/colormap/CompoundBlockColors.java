package net.mehvahdjukaar.polytone.colormap;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Keyable;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.mehvahdjukaar.polytone.lightmap.Lightmap;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// basically a map of colormap to tint color
public class CompoundBlockColors implements BlockColor {

    final Int2ObjectMap<BlockColor> getters = new Int2ObjectArrayMap<>();

    protected static final Codec<CompoundBlockColors> DIRECT_CODEC = Lightmap.validate(Codec.simpleMap(Codec.STRING, Colormap.CODEC,
                            Keyable.forStrings(() -> IntStream.rangeClosed(-1, 16).mapToObj(String::valueOf)))
                    .xmap(CompoundBlockColors::new, CompoundBlockColors::toStringMap).codec(),
            c -> {
                if (c.getters.size() == 0) {
                    return DataResult.error( "Must have at least 1 tint getter");
                }
                return DataResult.success(c);
            });

    // single or multiple
    public static final Codec<BlockColor> CODEC = Codec.either(DIRECT_CODEC, Colormap.CODEC)
            .xmap(either -> either.map(Function.identity(), CompoundBlockColors::new), Either::right);

    private CompoundBlockColors(Map<String, BlockColor> map) {
        for (var e : map.entrySet()) {
            getters.put(Integer.parseInt(e.getKey()), e.getValue());
        }
    }

    private CompoundBlockColors() {
    }

    public CompoundBlockColors(BlockColor colormap) {
        getters.put(-1, colormap);
    }



    // default biome sample vanilla implementation
    public static CompoundBlockColors createDefault(Set<Integer> tintIndexes, boolean triangular) {
        var c = new CompoundBlockColors();
        for (var i : tintIndexes) {
            c.getters.put(i.intValue(), triangular ? Colormap.defTriangle() : Colormap.defSquare());
        }
        return c;
    }


    public Int2ObjectMap<BlockColor> getGetters() {
        return getters;
    }

    public Map<String, BlockColor> toStringMap() {
        return getters.int2ObjectEntrySet().stream()
                .collect(Collectors.toMap(entry -> String.valueOf(entry.getIntKey()), Map.Entry::getValue));
    }

    @Override
    public int getColor(@Nullable BlockState blockState, @Nullable BlockAndTintGetter level, @Nullable BlockPos blockPos, int tintIndex) {
        BlockColor getter = getters.get(tintIndex);
        if (getter == null) {
            getter = getters.get(-1);
        }
        if (getter != null) {
            return getter.getColor(blockState, level, blockPos, tintIndex);
        }

        return -1;
    }


}
