package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.expressions.PolyExpType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface IBlockExp {

    Codec<IBlockExp> CODEC = Codec.lazyInitialized(() -> scriptCodec(BlockExp.TYPE));
    Codec<IBlockExp> CODEC_RAND_SEEDED = Codec.lazyInitialized(() -> scriptCodec(BlockExp.TYPE_RAND_SEEDED));

    private static Codec<IBlockExp> scriptCodec(PolyExpType<BlockExp> type) {
        return SchemaCodecs.labeled(
                SchemaCodecs.alternatives(
                        CodecUtils.LENIENT_DOUBLE.xmap(IBlockExp::constant, i -> 0.0),
                        type.codec()),
                SchemaCodecs.alt("constant", Codec.DOUBLE),
                SchemaCodecs.alt("expression", type.codec()));
    }

    double evaluate(ClientLevel level, Vec3 pos, @Nullable BlockState state);

    // Variant that binds v, an externally supplied value
    default double evaluate(ClientLevel level, Vec3 pos, @Nullable BlockState state, double v) {
        return evaluate(level, pos, state);
    }

    //for biome blending in env attr
    default double evaluate(ClientLevel level, Vec3 pos, @Nullable BlockState state, @Nullable Biome biome, double v) {
        return evaluate(level, pos, state, v);
    }

    static IBlockExp constant(double value) {
        return (level, pos, state) -> value;
    }

    IBlockExp ZERO = constant(0.0);
    IBlockExp ONE = constant(1.0);
    IBlockExp PARTICLE_RAND = (a, b, c) -> (Math.random() * 2 - 1) * 0.4;

}
