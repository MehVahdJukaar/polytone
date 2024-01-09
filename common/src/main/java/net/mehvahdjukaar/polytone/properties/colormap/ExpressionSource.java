package net.mehvahdjukaar.polytone.properties.colormap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public final class ExpressionSource {

    private static final RandomSource RANDOM_SOURCE = RandomSource.create();

    //Keywords
    private static final String TEMPERATURE = "TEMPERATURE";
    private static final String DOWNFALL = "DOWNFALL";
    private static final String POS_X = "POS_X";
    private static final String POS_Y = "POS_Y";
    private static final String POS_Z = "POS_Z";

    private static final Function STATE_PROP = new Function("state_prop", 1) {
        @Override
        public double apply(double... args) {
            List<Property<?>> properties = new ArrayList<>(stateHack.getProperties());
            int index = (int) args[0];
            Property<?> p = properties.get(Mth.clamp(index, 0, properties.size() - 1));
            List<?> values = new ArrayList<>(p.getPossibleValues());
            return values.indexOf(stateHack.getValue(p));
        }
    };


    public static final Function RAND = new Function("rand", 0) {
        @Override
        public double apply(double... args) {
            return RANDOM_SOURCE.nextFloat();
        }
    };

    public static final Function COS = new Function("cos", 1) {
        @Override
        public double apply(double... args) {
            return Mth.cos((float) args[0]);
        }
    };

    public static final Function SIN = new Function("sin", 1) {
        @Override
        public double apply(double... args) {
            return Mth.sin((float) args[0]);
        }
    };

    private static BlockState stateHack = null;


    public static final Codec<ExpressionSource> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            Expression compiled = createExpression(s);
            return DataResult.success(new ExpressionSource(compiled, s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.unparsed));

    private static Expression createExpression(String s) {
        return new ExpressionBuilder(s)
                .functions(STATE_PROP, RAND, SIN, COS)
                .variables(TEMPERATURE, DOWNFALL, POS_X, POS_Y, POS_Z)
                .build();
    }

    private final String unparsed;
    private final Expression expression;
    private final boolean hasX;
    private final boolean hasY;
    private final boolean hasZ;
    private final boolean hasT;
    private final boolean hasD;

    private ExpressionSource(Expression expression, String unparsed) {
        this.expression = expression;
        this.unparsed = unparsed;
        this.hasX = unparsed.contains(POS_X);
        this.hasY = unparsed.contains(POS_Y);
        this.hasZ = unparsed.contains(POS_Z);
        this.hasT = unparsed.contains(TEMPERATURE);
        this.hasD = unparsed.contains(DOWNFALL);
    }

    //Unckecked
    public static ExpressionSource make(String s) {
        return new ExpressionSource(createExpression(s), s);
    }


    public float getValue(BlockState state, @NotNull BlockAndTintGetter level, @NotNull BlockPos pos) {

        try {
            expression.setVariable(TEMPERATURE, hasT ? level.getBlockTint(pos, Colormap.TEMPERATURE_RESOLVER) : 0);
            expression.setVariable(DOWNFALL, hasD ? level.getBlockTint(pos, Colormap.DOWNFALL_RESOLVER) : 0);
            expression.setVariable(POS_X, hasX ? pos.getX() : 0);
            expression.setVariable(POS_Y, hasY ? pos.getY() : 0);
            expression.setVariable(POS_Z, hasZ ? pos.getZ() : 0);

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
