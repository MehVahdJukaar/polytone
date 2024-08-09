package net.mehvahdjukaar.polytone.colormap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

// basically a map of colormap to tint color
public class IndexCompoundColorGetter implements IColorGetter {

    final Int2ObjectMap<IColorGetter> getters = new Int2ObjectArrayMap<>();

    private IndexCompoundColorGetter(Map<Integer, IColorGetter> map) {
        getters.putAll(map);
    }

    private IndexCompoundColorGetter() {
    }

    protected static final Codec<IndexCompoundColorGetter> DIRECT_CODEC = ExtraCodecs.validate(Codec.unboundedMap(Codec.STRING
                                    .flatXmap(s -> {
                                        try {
                                            int i = Integer.parseInt(s);
                                            return DataResult.success(i);
                                        } catch (NumberFormatException e) {
                                            return DataResult.error(() -> "Not a valid integer: " + s);
                                        }
                                    }, i -> DataResult.success(String.valueOf(i))),
                            Colormap.CODEC)
                    .xmap(IndexCompoundColorGetter::new, comp -> comp.getters),
            c -> {
                if (c.getters.isEmpty()) {
                    return DataResult.error(() -> "Must have at least 1 tint getter");
                }
                return DataResult.success(c);
            });

    // single or multiple
    public static final Codec<IColorGetter> SINGLE_OR_MULTIPLE = Codec.withAlternative(Colormap.CODEC, DIRECT_CODEC,
            iColorGetter -> iColorGetter);

    // default biome sample vanilla implementation
    public static IndexCompoundColorGetter createDefault(Set<Integer> tintIndexes, boolean triangular) {
        var c = new IndexCompoundColorGetter();
        for (var i : tintIndexes) {
            c.getters.put(i.intValue(), triangular ? Colormap.createDefTriangle() : Colormap.createDefSquare());
        }
        return c;
    }


    public Int2ObjectMap<IColorGetter> getGetters() {
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


    @Override
    public int getColor(ItemStack itemStack, int i) {
        ItemColor getter = getters.get(i);
        if (getter == null) {
            getter = getters.get(-1);
        }
        if (getter != null) {
            return getter.getColor(itemStack, i);
        }

        return -1;
    }
}
