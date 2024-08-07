package net.mehvahdjukaar.polytone.colormap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.biome.BiomeIdMapper;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public final class ColormapExpressionProvider implements IColormapNumberProvider {

    //Keywords
    private static final String TEMPERATURE = "TEMPERATURE";
    private static final String DOWNFALL = "DOWNFALL";
    private static final String POS_X = "POS_X";
    private static final String POS_Y = "POS_Y";
    private static final String POS_Z = "POS_Z";
    private static final String BIOME_VALUE = "BIOME_VALUE";
    private static final String DAMAGE = "DAMAGE";

    private static final String TIME = "TIME";
    private static final String DAY_TIME = "DAY_TIME";
    private static final String RAIN = "RAIN";

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


    public static final Codec<ColormapExpressionProvider> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            Expression compiled = createExpression(s);
            return DataResult.success(new ColormapExpressionProvider(compiled, s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.unparsed));

    private static Expression createExpression(String s) {
        return new ExpressionBuilder(ExpressionUtils.removeHex(s))
                .functions(ExpressionUtils.defFunc(STATE_PROP, STATE_PROP_INT))
                .variables(TEMPERATURE, DOWNFALL, POS_X, POS_Y, POS_Z, BIOME_VALUE, TIME, RAIN, DAY_TIME)
                .operator(ExpressionUtils.defOp())
                .build();
    }

    private final String unparsed;
    private final Expression expression;

    // we use this optimistic approach instead of a lock because it's faster,
    // and we don't really care about blocking as we can just use new if its locked
    private final AtomicBoolean nonBlockingLock = new AtomicBoolean();

    private final boolean hasTemperature;
    private final boolean hasDownfall;
    private final boolean hasRain;
    private final boolean hasTime;
    private final boolean hasDayTime;

    private ColormapExpressionProvider(Expression expression, String unparsed) {
        this.expression = expression;
        this.unparsed = unparsed;

        this.hasTemperature = unparsed.contains(TEMPERATURE);
        this.hasDownfall = unparsed.contains(DOWNFALL);
        this.hasRain = unparsed.contains(RAIN);
        this.hasTime = unparsed.contains(TIME);
        this.hasDayTime = unparsed.contains(DAY_TIME);
    }

    //Unckecked
    public static ColormapExpressionProvider make(String s) {
        return new ColormapExpressionProvider(createExpression(s), s);
    }

    @Override
    public boolean usesBiome() {
        return unparsed.contains(TEMPERATURE) || unparsed.contains(DOWNFALL)
                || unparsed.contains(BIOME_VALUE);
    }

    @Override
    public boolean usesPos() {
        return unparsed.contains(POS_X) || unparsed.contains(POS_Y) || unparsed.contains(POS_Z);
    }

    @Override
    public boolean usesState() {
        return unparsed.contains(STATE_FUNC);
    }

    @Override
    public float getValue(@Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome,
                          @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
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

            if (hasTemperature)
                exp.setVariable(TEMPERATURE, biome != null ? ColorUtils.getClimateSettings(biome).temperature : 0);
            if (hasDownfall)
                exp.setVariable(DOWNFALL, biome != null ? ColorUtils.getClimateSettings(biome).downfall : 0);

            exp.setVariable(POS_X, pos != null ? pos.getX() : 0);
            exp.setVariable(POS_Y, pos != null ? pos.getY() : 0);
            exp.setVariable(POS_Z, pos != null ? pos.getZ() : 0);

            if (hasRain) exp.setVariable(RAIN, ClientFrameTicker.getRainAndThunder());
            if (hasTime) exp.setVariable(TIME, ClientFrameTicker.getGameTime());
            if (hasDayTime) exp.setVariable(DAY_TIME, ClientFrameTicker.getDayTime());

            if (stack != null) {
                float damage = 1 - stack.getDamageValue() / (float) stack.getMaxDamage();
                exp.setVariable(DAMAGE, damage);
            } else exp.setVariable(DAMAGE, 0);


            // Evaluate the expression
            //this state hack won't even work as its multithreaded lmao

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
