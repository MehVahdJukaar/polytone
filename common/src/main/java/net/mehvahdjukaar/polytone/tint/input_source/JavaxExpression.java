package net.mehvahdjukaar.polytone.tint.input_source;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;


public final class JavaxExpression implements InputSource {

    //Keywords
    private static final String TEMPERATURE = "TEMPERATURE";
    private static final String DOWNFALL = "DOWNFALL";
    private static final String POS_X = "POS_X";
    private static final String POS_Y = "POS_Y";
    private static final String POS_Z = "POS_Z";

    public static final Codec<JavaxExpression> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            Expression compiled =  new ExpressionBuilder(s)
                    .variables(TEMPERATURE, DOWNFALL, POS_X, POS_Y, POS_Z)
                    .build();
            return DataResult.success(new JavaxExpression(compiled, s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.unparsed));

    private final String unparsed;
    private final Expression expression;

    private JavaxExpression(Expression expression, String unparsed) {
        this.expression = expression;
        this.unparsed = unparsed;
    }

    @Override
    public Codec<JavaxExpression> getCodec() {
        return CODEC;
    }

    @Override
    public float getValue(BlockState state, BlockAndTintGetter level, BlockPos pos) {

        try {
            if (unparsed.contains(TEMPERATURE)) {
                expression.setVariable(TEMPERATURE, level.getBlockTint(pos, InputSources.TEMPERATURE_RESOLVER));
            }
            if (unparsed.contains(DOWNFALL)) {
                expression.setVariable(DOWNFALL, level.getBlockTint(pos, InputSources.DOWNFALL_RESOLVER));
            }
            if (unparsed.contains(POS_X)) {
                expression.setVariable(POS_X, pos.getX());
            }
            if (unparsed.contains(POS_Y)) {
                expression.setVariable(POS_Y, pos.getY());
            }
            if (unparsed.contains(POS_Z)) {
                expression.setVariable(POS_Z, pos.getZ());
            }
            // Evaluate the expression
            double result =  expression.evaluate();
            return (float)result;
        } catch (Exception e) {
            Polytone.LOGGER.error("Failed to evaluate expression with value: {}", unparsed, e);
        }
        return -1;
    }
}
