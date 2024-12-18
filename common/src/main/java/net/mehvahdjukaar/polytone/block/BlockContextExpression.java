package net.mehvahdjukaar.polytone.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.mehvahdjukaar.polytone.utils.exp.ConcurrentExpression;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BlockContextExpression {


    private final ConcurrentExpression expression;
    private final String unparsed;

    private final boolean hasTime;
    private final boolean hasRain;
    private final boolean hasX;
    private final boolean hasY;
    private final boolean hasZ;
    private final boolean hasState;
    private final boolean hasDayTime;
    private final boolean hasSkyLight;
    private final boolean hasBlockLight;

    private static final String POS_X = "POS_X";
    private static final String POS_Y = "POS_Y";
    private static final String POS_Z = "POS_Z";

    private static final String RAIN = "RAIN";
    private static final String TIME = "TIME";
    private static final String DAY_TIME = "DAY_TIME";
    private static final String BLOCK_LIGHT = "BLOCK_LIGHT";
    private static final String SKY_LIGHT = "SKY_LIGHT";
    private static final String DISTANCE_SQUARED = "DISTANCE_SQUARED";


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

    public static final Codec<BlockContextExpression> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            return DataResult.success(new BlockContextExpression(s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.unparsed));


    private static ConcurrentExpression createExpression(String s) {
        return ConcurrentExpression.of(new ExpressionBuilder(s)
                .functions(ExpressionUtils.defFunc(STATE_PROP, STATE_PROP_INT))
                .variables( POS_X, POS_Y, POS_Z, RAIN, DAY_TIME, TIME, BLOCK_LIGHT, SKY_LIGHT, DISTANCE_SQUARED)
                .operator(ExpressionUtils.defOp())
        );
    }

    private final boolean hasDistance;

    public BlockContextExpression(String expression) {
        this(createExpression(expression), expression);
    }

    public BlockContextExpression(ConcurrentExpression expression, String unparsed) {
        this.expression = expression;
        this.unparsed = unparsed;
        this.hasTime = unparsed.contains(TIME);
        this.hasX = unparsed.contains(POS_X);
        this.hasY = unparsed.contains(POS_Y);
        this.hasZ = unparsed.contains(POS_Z);
        this.hasState = unparsed.contains(STATE_FUNC);
        this.hasRain = unparsed.contains(RAIN);
        this.hasDayTime = unparsed.contains(DAY_TIME);
        this.hasSkyLight = unparsed.contains(SKY_LIGHT);
        this.hasBlockLight = unparsed.contains(BLOCK_LIGHT);
        this.hasDistance = unparsed.contains(DISTANCE_SQUARED);
    }

    //TODO: turn into entity context expression
    public double getValue(Vec3 pos, float entityTime) {
        ExpressionUtils.randomizeRandom();
        if (hasX) expression.setVariable(POS_X, pos.x);
        if (hasY) expression.setVariable(POS_Y, pos.y);
        if (hasZ) expression.setVariable(POS_Z, pos.z);
        if (hasTime) expression.setVariable(TIME, entityTime);
        if (hasRain) expression.setVariable(RAIN, ClientFrameTicker.getRainAndThunder());
        if (hasDayTime) expression.setVariable(DAY_TIME, ClientFrameTicker.getDayTime());
        if (hasDistance) {
            var e = Minecraft.getInstance().getCameraEntity();
            double x = pos.x - e.getX();
            double y = pos.y - e.getY();
            double z = pos.z - e.getZ();
            expression.setVariable(DISTANCE_SQUARED, x * x + y * y + z * z);
        }
        return expression.evaluate();
    }

    public double getValue(Level level, @NotNull BlockPos pos, BlockState state) {
        ExpressionUtils.seedRandom(pos.hashCode() * pos.asLong());
        if (hasX) expression.setVariable(POS_X, pos.getX());
        if (hasY) expression.setVariable(POS_Y, pos.getY());
        if (hasZ) expression.setVariable(POS_Z, pos.getZ());
        if (hasTime) expression.setVariable(TIME, ClientFrameTicker.getGameTime());
        if (hasRain) expression.setVariable(RAIN, ClientFrameTicker.getRainAndThunder());
        if (hasDayTime) expression.setVariable(DAY_TIME, ClientFrameTicker.getDayTime());
        if (hasSkyLight) expression.setVariable(SKY_LIGHT, level.getBrightness(LightLayer.SKY, pos));
        if (hasBlockLight) expression.setVariable(BLOCK_LIGHT, level.getBrightness(LightLayer.BLOCK, pos));
        if (hasState) STATE_HACK.set(state);
        return expression.evaluate();
    }

    public static final BlockContextExpression ZERO = new BlockContextExpression("0");
    public static final BlockContextExpression ONE = new BlockContextExpression("1");
    public static final BlockContextExpression PARTICLE_RAND= new BlockContextExpression("(rand() * 2.0 - 1.0) * 0.4");
}
