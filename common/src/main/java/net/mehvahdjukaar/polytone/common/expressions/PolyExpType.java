package net.mehvahdjukaar.polytone.common.expressions;


import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import hollowpoint.nexp.api.ExpCompletion;
import hollowpoint.nexp.api.ExpEngine;
import hollowpoint.nexp.api.ExpProgram;
import hollowpoint.nexp.api.ExpScope;
import net.mehvahdjukaar.polytone.Polytone;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class PolyExpType<T extends PolyExp> {

    //sandbox package
    private static final ExpEngine ENGINE = new ExpEngine(
            "net.mehvahdjukaar.polytone.common.expressions"
    ).importStatic(ExpMath.class);

    private final BiFunction<ExpProgram, String, T> constructor;
    private final ExpScope inputs;
    private final Codec<T> codec = Codec.STRING.flatXmap(this::create, exp -> DataResult.success(exp.source));

    public PolyExpType(BiFunction<ExpProgram, String, T> constructor, Consumer<ExpScope> inputs) {
        this.constructor = constructor;
        this.inputs = ExpUtils.commonScope();
        inputs.accept(this.inputs);
    }

    public Codec<T> codec() {
        return codec;
    }

    private ExpScope scope() {
        ExpScope scope = inputs.copy();
        Polytone.GLOBAL_EXPRESSION.slots().forEach(scope::number);
        return scope;
    }

    public DataResult<T> create(String source) {
        try {
            return DataResult.success(constructor.apply(ENGINE.compile(source, scope()), source));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to compile expression: " + e.getMessage());
        }
    }

    public List<String> inputNames() {
        return scope().names();
    }

    public List<ExpCompletion> complete(String source, int caret) {
        return ENGINE.complete(source, caret, scope());
    }

    public static List<String> functionNames() {
        return ENGINE.functions();
    }

    public static List<String> constantNames() {
        return ENGINE.constants();
    }
}
