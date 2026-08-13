package net.mehvahdjukaar.polytone.content.common.expressions;


import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import java.io.Serializable;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PolyExpType<T extends PolyExp> {

    private final BiFunction<Serializable, String, T> constructor;
    private final ParserContext context;
    private final Codec<T> codec = Codec.STRING.flatXmap(
            this::create,
            exp -> DataResult.success("0") //unsupported
    );
    public PolyExpType( Function<Serializable, T> constructor, Consumer<ParserContext> inputs) {
        this( (expr, str) -> constructor.apply(expr), inputs);
    }

    public PolyExpType( BiFunction<Serializable, String, T> constructor, Consumer<ParserContext> inputs) {
        this.constructor = constructor;
        this.context = new ParserContext();
        this.context.setStrongTyping(true);
        this.context.setStrictTypeEnforcement(true);
        inputs.accept(this.context);
    }

    public Codec<T> codec() {
        return codec;
    }

    public List<String> inputNames() {
        var inputs = context.getInputs();
        return inputs == null ? List.of()
                : inputs.keySet().stream().sorted().toList();
    }

    public DataResult<T> create(String expressionStr) {
        try {
            String upgraded = ExpUtils.upgrade(expressionStr);
            Serializable expr = MVEL.compileExpression(upgraded, this.context);
            T result = constructor.apply(expr, upgraded);
            result.unparsed = expressionStr; // keep the original for readable error messages
            return DataResult.success(result);
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to compile expression: " + e.getMessage());
        }
    }

}
