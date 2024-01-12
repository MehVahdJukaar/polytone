package net.mehvahdjukaar.polytone.colormap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public final class ExpressionSource {

    //Keywords
    private static final String TEMPERATURE = "TEMPERATURE";
    private static final String DOWNFALL = "DOWNFALL";
    private static final String POS_X = "POS_X";
    private static final String POS_Y = "POS_Y";
    private static final String POS_Z = "POS_Z";

    private static final Function STATE_PROP = new Function("state_prop", 1) {
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
                .functions(ExpressionUtils.defFunc(STATE_PROP))
                .variables(TEMPERATURE, DOWNFALL, POS_X, POS_Y, POS_Z)
                .operator(ExpressionUtils.defOp())
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

    // we use this optimistic approach instead of a lock because it's faster,
    // and we don't really care about blocking as we can just use new if its locked
    private final AtomicBoolean nonBlockingLock = new AtomicBoolean();

    public float getValue(BlockState state, @NotNull BlockAndTintGetter level, @NotNull BlockPos pos) {
        float result = 0;
        boolean needsToUnlock = false;
        try {
            Expression exp;
            // no other code has acquired this yet so we can use our instance
            if (false && nonBlockingLock.compareAndSet(false, true)) {
                exp = expression;
                needsToUnlock = true;
            } else {
                // if not, we have to create a new one because this has to work concurrently.
                exp = new Expression(this.expression);
            }

            exp.setVariable(TEMPERATURE, hasT ? level.getBlockTint(pos, Colormap.TEMPERATURE_RESOLVER) : 0);
            exp.setVariable(DOWNFALL, hasD ? level.getBlockTint(pos, Colormap.DOWNFALL_RESOLVER) : 0);
            exp.setVariable(POS_X, hasX ? pos.getX() : 0);
            exp.setVariable(POS_Y, hasY ? pos.getY() : 0);
            exp.setVariable(POS_Z, hasZ ? pos.getZ() : 0);

            // Evaluate the expression
            //this state hack wont even work as its multithreaded lmao
            STATE_HACK.set(state);
            result = (float) exp.evaluate();
            STATE_HACK.remove();

        } catch (Exception e) {
            Polytone.LOGGER.error("Failed to evaluate expression with value: {}", unparsed, e);
        } finally {
            if (needsToUnlock) nonBlockingLock.set(false);
        }
        return result;
    }
}
