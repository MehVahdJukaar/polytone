package net.mehvahdjukaar.polytone.tint.input_source;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.script.*;

public final class JavaxExpression implements InputSource {

    private static final ScriptEngine ENGINE = new ScriptEngineManager().getEngineByName("JavaScript");

    //Keywords
    private static final String TEMPERATURE = "TEMPERATURE";
    private static final String DOWNFALL = "DOWNFALL";
    private static final String POS_X = "POS_X";
    private static final String POS_Y = "POS_Y";
    private static final String POS_Z = "POS_Z";

    public static final Codec<JavaxExpression> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            CompiledScript compiled = ((Compilable) ENGINE).compile(s);
            return DataResult.success(new JavaxExpression(compiled, s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, javaxExpression -> DataResult.success(javaxExpression.unparsed));

    private final String unparsed;
    private final CompiledScript expression;

    private JavaxExpression(CompiledScript expression, String unparsed) {
        this.expression = expression;
        this.unparsed = unparsed;
    }

    @Override
    public Codec<JavaxExpression> getCodec() {
        return  CODEC;
    }

    @Override
    public float getValue(BlockState state, BlockAndTintGetter level, BlockPos pos) {

        try {
            Bindings variables = new SimpleBindings();
            if (unparsed.contains(TEMPERATURE)) {
                variables.put(TEMPERATURE, level.getBlockTint(pos, InputSources.TEMPERATURE_RESOLVER));
            }
            if (unparsed.contains(DOWNFALL)) {
                variables.put(DOWNFALL, level.getBlockTint(pos, InputSources.DOWNFALL_RESOLVER));
            }
            if (unparsed.contains(POS_X)) {
                variables.put(POS_X, pos.getX());
            }
            if (unparsed.contains(POS_Y)) {
                variables.put(POS_Y, pos.getY());
            }
            if (unparsed.contains(POS_Z)) {
                variables.put(POS_Z, pos.getZ());
            }
            // Evaluate the expression
            Number result = (Number) expression.eval(variables);
            return result.floatValue();
        } catch (ScriptException e) {
            Polytone.LOGGER.error("Failed to evaluate expression with value: {}", unparsed, e);
        }
        return -1;
    }
}
