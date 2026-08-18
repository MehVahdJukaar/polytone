package net.mehvahdjukaar.polytone.content.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.content.block.BlockContextExpression;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface IBlockExp {

    Codec<IBlockExp> CODEC = Codec.lazyInitialized(() -> SchemaCodecs.labeled(
            SchemaCodecs.alternatives(
                    CodecUtils.LENIENT_DOUBLE.xmap(
                            aDouble -> (level, pos, state) -> aDouble,
                            i -> 0.0
                    ),
                    BlockContextExpression.CODEC.xmap(
                            // wrap the existing 1.21.1 block expression so it satisfies the new interface
                            bce -> (level, pos, state) -> {
                                if (level instanceof Level l) {
                                    return bce.getValue(l, BlockPos.containing(pos), state);
                                }
                                return 0.0;
                            },
                            i -> BlockContextExpression.ZERO
                    ),
                    BlockExp.TYPE.codec()),
            // constant: plain number (LENIENT_DOUBLE would splice its double-or-string union into
            // stray "number"/"text" options). expression before legacy: both encode as bare strings,
            // so fit-scoring on load should land on the modern branch, not the deprecated one.
            SchemaCodecs.alt("constant", Codec.DOUBLE),
            SchemaCodecs.alt("expression", BlockExp.TYPE.codec()),
            SchemaCodecs.alt("legacy expression", BlockContextExpression.CODEC))
    );

    // for new systems: constant or MVEL only, no exp4j legacy branch
    Codec<IBlockExp> MVEL_CODEC = Codec.lazyInitialized(() -> SchemaCodecs.labeled(
            SchemaCodecs.alternatives(
                    CodecUtils.LENIENT_DOUBLE.xmap(IBlockExp::constant, i -> 0.0),
                    BlockExp.TYPE.codec()),
            SchemaCodecs.alt("constant", Codec.DOUBLE),
            SchemaCodecs.alt("expression", BlockExp.TYPE.codec()))
    );

    double evaluate(LevelReader level, Vec3 pos, @Nullable BlockState state);

    static IBlockExp constant(double value) {
        return (level, pos, state) -> value;
    }

    IBlockExp ZERO = constant(0.0);
    IBlockExp ONE = constant(1.0);
    IBlockExp PARTICLE_RAND = (a, b, c) -> (Math.random() * 2 - 1) * 0.4;

}
