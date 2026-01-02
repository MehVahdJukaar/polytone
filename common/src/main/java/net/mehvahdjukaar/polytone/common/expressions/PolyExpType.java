package net.mehvahdjukaar.polytone.common.expressions;


import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PolyExpType<T extends PolyExp> {

    private final Function<Serializable, T> constructor;
    private final ParserContext context;
    private final Codec<T> codec = Codec.STRING.flatXmap(
            this::create,
            exp -> DataResult.success("0") //unsupported
    );

    public PolyExpType(Function<Serializable, T> constructor, Consumer<ParserContext> inputs) {
        this.constructor = constructor;
        ParserContext ctx = new ParserContext();
        ctx.setStrongTyping(true);
        ctx.setStrictTypeEnforcement(true);
        inputs.accept(ctx);
    }

    public Codec<T> codec() {
        return codec;
    }

    private void buildContext(ParserContext ctx) {

    //    ctx.addInput("price", int.class);
     //   ctx.addInput("category", String.class);
       // ctx.addImport("BigDecimal", BigDecimal.class);
      //  ctx.addImport("time", MVEL.getStaticMethod(System.class, "currentTimeMillis", new Class[0]));
    }

    public DataResult<T> create(String expressionStr) {
        try {
            expressionStr = ExpUtils.upgrade(expressionStr);
            Serializable expr = MVEL.compileExpression(expressionStr, this.context);
            return DataResult.success(constructor.apply(expr));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to compile expression: " + e.getMessage());
        }
    }

}
