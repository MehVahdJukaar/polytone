package net.mehvahdjukaar.polytone.utils.input_source;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.properties.Colormap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

import java.util.ArrayList;
import java.util.List;


public final class ExpressionSource implements InputSource {

    private static final RandomSource RANDOM_SOURCE = RandomSource.create();

    //Keywords
    private static final String TEMPERATURE = "TEMPERATURE";
    private static final String DOWNFALL = "DOWNFALL";
    private static final String POS_X = "POS_X";
    private static final String POS_Y = "POS_Y";
    private static final String POS_Z = "POS_Z";

    private static final Function PROP_VALUE = new Function("prop_value", 1) {
        @Override
        public double apply(double... args) {
            List<Property<?>> properties = new ArrayList<>(stateHack.getProperties());
            int index = (int) args[0];
            Property<?> p = properties.get(Mth.clamp(index, 0, properties.size() - 1));
            List<?> values = new ArrayList<>(p.getPossibleValues());
            return values.indexOf(stateHack.getValue(p));
        }
    };


    private static final Function RAND = new Function("rand", 0) {
        @Override
        public double apply(double... args) {
            return RANDOM_SOURCE.nextFloat();
        }
    };

    private static BlockState stateHack = null;


    public static final Codec<ExpressionSource> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            Expression compiled = new ExpressionBuilder(s)
                    .functions(PROP_VALUE, RAND)
                    .variables(TEMPERATURE, DOWNFALL, POS_X, POS_Y, POS_Z)
                    .build();
            return DataResult.success(new ExpressionSource(compiled, s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.unparsed));

    private final String unparsed;
    private final Expression expression;

    private ExpressionSource(Expression expression, String unparsed) {
        this.expression = expression;
        this.unparsed = unparsed;
    }

    @Override
    public Codec<ExpressionSource> getCodec() {
        return CODEC;
    }

    @Override
    public float getValue(BlockState state, BlockAndTintGetter level, BlockPos pos) {

        try {
            if (unparsed.contains(TEMPERATURE)) {
                int blockTint = level.getBlockTint(pos, Colormap.TEMPERATURE_RESOLVER);
                expression.setVariable(TEMPERATURE, blockTint);
            }
            if (unparsed.contains(DOWNFALL)) {
                int blockTint = level.getBlockTint(pos, Colormap.DOWNFALL_RESOLVER);
                expression.setVariable(DOWNFALL, blockTint);
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
            stateHack = state;
            double result = expression.evaluate();
            stateHack = null;
            return (float) result;
        } catch (Exception e) {
            Polytone.LOGGER.error("Failed to evaluate expression with value: {}", unparsed, e);
        }
        return 0;
    }
}
