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
            SchemaCodecs.alt("constant", CodecUtils.LENIENT_DOUBLE),
            SchemaCodecs.alt("legacy expression", BlockContextExpression.CODEC),
            SchemaCodecs.alt("expression", BlockExp.TYPE.codec()))
    );

    double evaluate(LevelReader level, Vec3 pos, @Nullable BlockState state);

    IBlockExp ZERO = (a, b, c) -> 0.0;
    IBlockExp ONE = (a, b, c) -> 1.0;
    IBlockExp PARTICLE_RAND = (a, b, c) -> (Math.random() * 2 - 1) * 0.4;

}
