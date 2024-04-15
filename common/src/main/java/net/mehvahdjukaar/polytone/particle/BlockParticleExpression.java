package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

import java.util.ArrayList;
import java.util.List;

public class BlockParticleExpression {


    private final Expression expression;
    private final String unparsed;

    private final boolean hasTime;
    private final boolean hasX;
    private final boolean hasY;
    private final boolean hasZ;
    private final boolean hasState;

    private static final String TIME = "TIME";
    private static final String POS_X = "POS_X";
    private static final String POS_Y = "POS_Y";
    private static final String POS_Z = "POS_Z";
    private static final String STATE_FUNC = "state_prop";
    private static final Function STATE_PROP = new Function(STATE_FUNC, 1) {
        @Override
        public double apply(double... args) {
            BlockState blockState = STATE_HACK.get();
            List<Property<?>> properties = new ArrayList<>(blockState.getProperties());
            int index = (int) args[0];
            Property<?> p = properties.get(Mth.clamp(index, 0, properties.size() - 1));
            List<?> values = new ArrayList<>(p.getPossibleValues());
            return values.indexOf(blockState.getValue(p)) / (properties.size() - 1f);
        }
    };

    private static final Function STATE_PROP_INT = new Function("state_prop_i", 1) {
        @Override
        public double apply(double... args) {
            BlockState blockState = STATE_HACK.get();
            List<Property<?>> properties = new ArrayList<>(blockState.getProperties());
            int index = (int) args[0];
            Property<?> p = properties.get(Mth.clamp(index, 0, properties.size() - 1));
            List<?> values = new ArrayList<>(p.getPossibleValues());
            return values.indexOf(blockState.getValue(p));
        }
    };


    private static final ThreadLocal<BlockState> STATE_HACK = new ThreadLocal<>();

    public static final Codec<BlockParticleExpression> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            return DataResult.success(new BlockParticleExpression(s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.unparsed));


    private static Expression createExpression(String s) {
        return new ExpressionBuilder(ExpressionUtils.removeHex(s))
                .functions(ExpressionUtils.defFunc(STATE_PROP, STATE_PROP_INT))
                .variables(TIME, POS_X, POS_Y, POS_Z)
                .operator(ExpressionUtils.defOp())
                .build();
    }

    public BlockParticleExpression(String expression){
        this(createExpression(expression), expression);
    }

    public BlockParticleExpression(Expression expression, String unparsed) {
        this.expression = expression;
        this.unparsed = unparsed;
        this.hasTime = unparsed.contains(TIME);
        this.hasX = unparsed.contains(POS_X);
        this.hasY = unparsed.contains(POS_Y);
        this.hasZ = unparsed.contains(POS_Z);
        this.hasState = unparsed.contains(STATE_FUNC);
    }

    public double getValue(Vec3 pos, float time) {
        if (hasTime) expression.setVariable(TIME, time);
        if (hasX) expression.setVariable(POS_X, pos.x);
        if (hasY) expression.setVariable(POS_Y, pos.y);
        if (hasZ) expression.setVariable(POS_Z, pos.z);
        return expression.evaluate();
    }

    public double getValue(Level level, BlockPos pos, BlockState state) {
        if (hasTime) expression.setVariable(TIME, level.getGameTime());
        if (hasX) expression.setVariable(POS_X, pos.getX());
        if (hasY) expression.setVariable(POS_Y, pos.getY());
        if (hasZ) expression.setVariable(POS_Z, pos.getZ());
        if (hasState) STATE_HACK.set(state);
        return expression.evaluate();
    }

    public static final BlockParticleExpression ZERO = new BlockParticleExpression("0");
    public static final BlockParticleExpression ONE = new BlockParticleExpression("1");
}
