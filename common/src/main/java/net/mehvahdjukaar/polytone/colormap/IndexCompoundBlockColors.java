package net.mehvahdjukaar.polytone.colormap;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

// basically a map of colormap to tint color
public class IndexCompoundBlockColors implements BlockColor {

    final Int2ObjectMap<BlockColor> getters = new Int2ObjectArrayMap<>();

    protected static final Codec<IndexCompoundBlockColors> DIRECT_CODEC = ExtraCodecs.validate(Codec.unboundedMap(Codec.STRING
                                    .flatXmap(s -> {
                                        try {
                                            int i = Integer.parseInt(s);
                                            return DataResult.success(i);
                                        } catch (NumberFormatException e) {
                                            return DataResult.error(() -> "Not a valid integer: " + s);
                                        }
                                    }, i -> DataResult.success(String.valueOf(i))),
                            Colormap.CODEC)
                    .xmap(IndexCompoundBlockColors::new, comp -> comp.getters),
            c -> {
                if (c.getters.size() == 0) {
                    return DataResult.error(() -> "Must have at least 1 tint getter");
                }
                return DataResult.success(c);
            });

    // single or multiple
    public static final Codec<BlockColor> CODEC = Codec.either(DIRECT_CODEC, Colormap.CODEC)
            .xmap(either -> either.map(Function.identity(), Function.identity()), Either::right);

    private IndexCompoundBlockColors(Map<Integer, BlockColor> map) {
        getters.putAll(map);
    }

    private IndexCompoundBlockColors() {
    }

    // default biome sample vanilla implementation
    public static IndexCompoundBlockColors createDefault(Set<Integer> tintIndexes, boolean triangular) {
        var c = new IndexCompoundBlockColors();
        for (var i : tintIndexes) {
            c.getters.put(i.intValue(), triangular ? Colormap.defTriangle() : Colormap.defSquare());
        }
        return c;
    }


    public Int2ObjectMap<BlockColor> getGetters() {
        return getters;
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
