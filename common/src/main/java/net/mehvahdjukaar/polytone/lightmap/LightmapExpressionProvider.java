package net.mehvahdjukaar.polytone.lightmap;


import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.colormap.ColormapExpressionProvider;
import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

record LightmapExpressionProvider(Expression expression, String unparsed) implements ILightmapNumberProvider {

    public static final Codec<LightmapExpressionProvider> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            return DataResult.success(create(s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, exp -> DataResult.success(exp.unparsed));

    private static final String TIME = "TIME";
    private static final String RAIN = "RAIN";
    private static final String THUNDER = "THUNDER";

    public static LightmapExpressionProvider create(String s) {
        return new LightmapExpressionProvider(new ExpressionBuilder(s)
                .variables(TIME, RAIN, THUNDER)
                .functions(ExpressionUtils.defFunc())
                .operator(ExpressionUtils.defOp())
                .build(), s);
    }

    @Override
    public float getValue(float time, float rain, float thunder) {
        expression.setVariable(TIME, time);
        expression.setVariable(RAIN, rain);
        expression.setVariable(THUNDER, thunder);
        return 0;
    }
}
