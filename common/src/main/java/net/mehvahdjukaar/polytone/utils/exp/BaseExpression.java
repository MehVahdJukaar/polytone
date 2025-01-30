package net.mehvahdjukaar.polytone.utils.exp;

import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

import java.util.*;

import static net.mehvahdjukaar.polytone.utils.ExpressionUtils.defFunc;

public abstract class BaseExpression {

    // global stuff
    protected static final String TIME = "TIME";
    protected static final String DAY_TIME = "DAY_TIME";
    protected static final String SUN_TIME = "SUN_TIME";
    protected static final String RAIN = "RAIN";

    // at pos stuff
    protected static final String POS_X = "POS_X";
    protected static final String POS_Y = "POS_Y";
    protected static final String POS_Z = "POS_Z";
    protected static final String SKY_LIGHT = "SKY_LIGHT";
    protected static final String BLOCK_LIGHT = "BLOCK_LIGHT";
    protected static final String TEMPERATURE = "TEMPERATURE";
    protected static final String DOWNFALL = "DOWNFALL";

    // player stuff
    protected static final String PLAYER_X = "PLAYER_X";
    protected static final String PLAYER_Y = "PLAYER_Y";
    protected static final String PLAYER_Z = "PLAYER_Z";
    protected static final String DISTANCE_SQUARED = "DISTANCE_SQUARED";
    protected static final String PLAYER_SPEED_SQUARED = "PLAYER_SPEED_SQUARED";

    protected static final String RENDER_DISTANCE = "RENDER_DISTANCE";

    // normally unused
    protected static final String STATE_FUNC = "state_prop";
    protected static final Function STATE_PROP = new Function(STATE_FUNC, 1) {
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

    protected static final Function STATE_PROP_INT = new Function("state_prop_i", 1) {
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

    protected static final ThreadLocal<BlockState> STATE_HACK = new ThreadLocal<>();

    private final String unparsed;
    protected final ConcurrentExpression expression;

    protected final boolean hasPos;

    protected final boolean hasTime;
    protected final boolean hasDayTime;
    protected final boolean hasSunTime;
    protected final boolean hasRain;

    protected final boolean hasSkyLight;
    protected final boolean hasBlockLight;
    protected final boolean hasTemperature;
    protected final boolean hasDownfall;

    protected final boolean hasPlayer;
    protected final boolean hasDistance;
    protected final boolean hasPlayerSpeed;

    protected final boolean hasRenderDistance;


    public BaseExpression(String unparsed) {
        FunBuilder funBuilder = new FunBuilder();
        buildFunctions(funBuilder);
        VarBuilder varBuilder = new VarBuilder();
        buildVars(varBuilder);
        this.expression = ConcurrentExpression.of(new ExpressionBuilder(unparsed)
                .functions(defFunc(funBuilder.list.toArray(new Function[0])))
                .variables(varBuilder.list)
                .operator(ExpressionUtils.defOp())
        );
        this.unparsed = unparsed;

        this.hasPos = unparsed.contains(POS_X) || unparsed.contains(POS_Y) || unparsed.contains(POS_Z);

        this.hasTime = unparsed.contains(TIME);
        this.hasDayTime = unparsed.contains(DAY_TIME);
        this.hasSunTime = unparsed.contains(SUN_TIME);
        this.hasRain = unparsed.contains(RAIN);

        this.hasSkyLight = unparsed.contains(SKY_LIGHT);
        this.hasBlockLight = unparsed.contains(BLOCK_LIGHT);
        this.hasTemperature = unparsed.contains(TEMPERATURE);
        this.hasDownfall = unparsed.contains(DOWNFALL);

        this.hasDistance = unparsed.contains(DISTANCE_SQUARED);
        this.hasPlayer = unparsed.contains(PLAYER_X) || unparsed.contains(PLAYER_Y) || unparsed.contains(PLAYER_Z);
        this.hasPlayerSpeed = unparsed.contains(PLAYER_SPEED_SQUARED);

        this.hasRenderDistance = unparsed.contains(RENDER_DISTANCE);
    }

    protected void buildVars(VarBuilder builder) {
        builder.addAll(RAIN, DAY_TIME, TIME, BLOCK_LIGHT, SKY_LIGHT, DISTANCE_SQUARED,
                PLAYER_X, PLAYER_Y, PLAYER_Z, PLAYER_SPEED_SQUARED);
    }

    protected void buildFunctions(FunBuilder builder) {
    }

    protected String getUnparsed() {
        return unparsed;
    }

    protected record VarBuilder(Set<String> list) {

        public VarBuilder() {
            this(new HashSet<>());
        }

        public void add(String s) {
            list.add(s);
        }

        public void addAll(String... s) {
            Collections.addAll(list, s);
        }
    }

    protected record FunBuilder(List<Function> list) {

        public FunBuilder() {
            this(new ArrayList<>());
        }

        public void add(Function f) {
            list.add(f);
        }

        public void addAll(Function... f) {
            Collections.addAll(list, f);
        }

        public Function[] unwrap() {
            return list.toArray(new Function[0]);
        }
    }
}
