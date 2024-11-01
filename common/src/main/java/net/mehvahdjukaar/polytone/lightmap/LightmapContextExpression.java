package net.mehvahdjukaar.polytone.lightmap;


import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.minecraft.core.BlockPos;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

record LightmapContextExpression(Expression expression, String unparsed,
                                 boolean usesBiome) implements ILightmapNumberProvider {

    public static final Codec<LightmapContextExpression> CODEC = Codec.STRING.flatXmap(s -> {
        try {
            return DataResult.success(create(s));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse expression:" + e.getMessage());
        }
    }, exp -> DataResult.success(exp.unparsed));

    private static final String TIME = "TIME";
    private static final String RAIN = "RAIN";
    private static final String THUNDER = "THUNDER";
    private static final String POS_X = "POS_X";
    private static final String POS_Y = "POS_Y";
    private static final String POS_Z = "POS_Z";
    private static final String TEMPERATURE = "TEMPERATURE";
    private static final String DOWNFALL = "DOWNFALL";

    public static LightmapContextExpression create(String s) {
        return new LightmapContextExpression(new ExpressionBuilder(ExpressionUtils.removeHex(s))
                .variables(TIME, RAIN, THUNDER, DOWNFALL, TEMPERATURE)
                .functions(ExpressionUtils.defFunc())
                .operator(ExpressionUtils.defOp())
                .build(), s,
                s.contains(TEMPERATURE) || s.contains(DOWNFALL));
    }

    @Override
    public double getValue(float time, float rain, float thunder) {
        expression.setVariable(TIME, time);
        expression.setVariable(RAIN, rain);
        expression.setVariable(THUNDER, thunder);
        BlockPos pos = ClientFrameTicker.getCameraPos();
        expression.setVariable(POS_X, pos.getX());
        expression.setVariable(POS_Y, pos.getY());
        expression.setVariable(POS_Z, pos.getZ());
        if (usesBiome) {
            var biome = ClientFrameTicker.getCameraBiome();
            if (biome == null) {
                expression.setVariable(TEMPERATURE, 0);
                expression.setVariable(DOWNFALL, 0);
            } else {
                var cs = ColorUtils.getClimateSettings(biome.value());
                expression.setVariable(TEMPERATURE, cs.temperature);
                expression.setVariable(DOWNFALL, cs.downfall);
            }

        }
        return expression.evaluate();
    }
}
