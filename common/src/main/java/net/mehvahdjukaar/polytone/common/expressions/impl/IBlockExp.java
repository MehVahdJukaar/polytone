package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.exp.impl.BlockContextExpression;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface IBlockExp {

    Codec<IBlockExp> CODEC_LEGACY = Codec.lazyInitialized(() -> SchemaCodecs.labeled(
            SchemaCodecs.alternatives(
                    CodecUtils.LENIENT_DOUBLE.xmap(
                            aDouble -> (IBlockExp) (level, pos, state) -> aDouble,
                            i -> 0.0
                    ),
                    BlockContextExpression.CODEC,
                    BlockExp.TYPE.codec()),
            SchemaCodecs.alt("constant", Codec.DOUBLE),
            SchemaCodecs.alt("expression", BlockExp.TYPE.codec()),
            SchemaCodecs.alt("legacy expression", BlockContextExpression.CODEC)));

    Codec<IBlockExp> CODEC = Codec.lazyInitialized(() -> SchemaCodecs.labeled(
            SchemaCodecs.alternatives(
                    CodecUtils.LENIENT_DOUBLE.xmap(IBlockExp::constant, i -> 0.0),
                    BlockExp.TYPE.codec()),
            SchemaCodecs.alt("constant", Codec.DOUBLE),
            SchemaCodecs.alt("expression", BlockExp.TYPE.codec())));

    double evaluate(LevelReader level, Vec3 pos, @Nullable BlockState state);

    static IBlockExp constant(double value) {
        return (level, pos, state) -> value;
    }

    IBlockExp ZERO = constant(0.0);
    IBlockExp ONE = constant(1.0);
    IBlockExp PARTICLE_RAND = (a, b, c) -> (Math.random() * 2 - 1) * 0.4;

}
