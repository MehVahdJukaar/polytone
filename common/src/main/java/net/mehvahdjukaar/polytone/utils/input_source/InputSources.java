package net.mehvahdjukaar.polytone.utils.input_source;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.properties.Colormap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.function.TriFunction;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class InputSources {
    private static final BiMap<String, Codec<InputSource>> TYPES = HashBiMap.create();

    public static final Codec<Codec<InputSource>> TYPE_CODEC = Codec.STRING.flatXmap(
            id -> Optional.ofNullable(TYPES.get(id)).map(DataResult::success)
                    .orElse(DataResult.error( "Unknown Input Source with id " + id)),
            object -> Optional.ofNullable(TYPES.inverse().get(object)).map(DataResult::success)
                    .orElse(DataResult.error( "Unknown Input Source: " + object)));

    public static final Codec<InputSource> CODEC = TYPE_CODEC
            .dispatch("type", i -> (Codec<InputSource>) i.getCodec(), Function.identity());


    public static Codec<InputSource> registerUnit(String id, TriFunction<BlockState, BlockAndTintGetter, BlockPos, Float> call) {
        AtomicReference<Codec<InputSource>> hack = new AtomicReference<>();
        InputSource instance = new InputSource() {
            @Override
            public Codec<InputSource> getCodec() {
                return hack.get();
            }

            @Override
            public float getValue(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                return call.apply(state, level, pos);
            }
        };
        Codec<InputSource> codec = Codec.unit(instance);
        hack.set(codec);
        return register(id, codec);
    }

    public static <T extends InputSource> Codec<T> register(String id, Codec<T> codec) {
        TYPES.put(id, (Codec<InputSource>) codec);
        return codec;
    }



    private static final Codec<InputSource> TEMPERATURE = registerUnit("temperature",
            (state, level, pos) -> Float.intBitsToFloat(level.getBlockTint(pos, Colormap. TEMPERATURE_RESOLVER)));

    private static final Codec<InputSource> DOWNFALL = registerUnit("downfall",
            (state, level, pos) -> Float.intBitsToFloat(level.getBlockTint(pos, Colormap.DOWNFALL_RESOLVER)));

    private static final Codec<InputSource> POS_X = registerUnit("pos_x", (state, level, pos) -> (float) pos.getX());
    private static final Codec<InputSource> POS_Y = registerUnit("pos_y", (state, level, pos) -> (float) pos.getY());
    private static final Codec<InputSource> POS_Z = registerUnit("pos_z", (state, level, pos) -> (float) pos.getZ());


    private static final Codec<ExpressionSource> JAVAX_EXPRESSION = register("expression", ExpressionSource.CODEC);
    //private static final InputSource SIN = registerUnit("sin", )
}
