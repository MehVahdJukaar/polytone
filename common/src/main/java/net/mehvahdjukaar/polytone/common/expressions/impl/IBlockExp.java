package net.mehvahdjukaar.polytone.common.expressions.impl;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.exp.impl.BlockContextExpression;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface IBlockExp {

    Codec<IBlockExp> CODEC = Codec.lazyInitialized(() -> SchemaCodecs.labeled(
            SchemaCodecs.alternatives(
                    CodecUtils.LENIENT_DOUBLE.xmap(
                            aDouble -> (IBlockExp) (level, pos, state) -> aDouble,
                            i -> 0.0
                    ),
                    BlockContextExpression.CODEC,
                    BlockExp.TYPE.codec()),
            // constant: plain number (LENIENT_DOUBLE would splice its double-or-string union into
            // stray "number"/"text" options). expression before legacy: both encode as bare strings,
            // so fit-scoring on load should land on the modern branch, not the deprecated one.
            SchemaCodecs.alt("constant", Codec.DOUBLE),
            SchemaCodecs.alt("expression", BlockExp.TYPE.codec()),
            SchemaCodecs.alt("legacy expression", BlockContextExpression.CODEC)));

    Codec<IBlockExp> MVEL_CODEC = Codec.lazyInitialized(() -> SchemaCodecs.labeled(
            SchemaCodecs.alternatives(
                    CodecUtils.LENIENT_DOUBLE.xmap(IBlockExp::constant, i -> 0.0),
                    BlockExp.TYPE.codec()),
            SchemaCodecs.alt("constant", Codec.DOUBLE),
            SchemaCodecs.alt("expression", BlockExp.TYPE.codec())));

    double evaluate(ClientLevel level, Vec3 pos, @Nullable BlockState state);

    // Variant that binds v, an externally supplied value (e.g. an expression-driven model selector's result).
    // Implementations that don't support it simply ignore the value.
    default double evaluate(ClientLevel level, Vec3 pos, @Nullable BlockState state, double v) {
        return evaluate(level, pos, state);
    }

    static IBlockExp constant(double value) {
        return (level, pos, state) -> value;
    }

    IBlockExp ZERO = constant(0.0);
    IBlockExp ONE = constant(1.0);
    IBlockExp PARTICLE_RAND = (a, b, c) -> (Math.random() * 2 - 1) * 0.4;

}
