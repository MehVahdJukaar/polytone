package net.mehvahdjukaar.polytone.content.lightmap;


import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.misc.ClientFrameTicker;
import net.mehvahdjukaar.polytone.misc.ColorUtils;
import net.mehvahdjukaar.polytone.misc.exp.ExpressionUtils;
import net.mehvahdjukaar.polytone.misc.exp.ConcurrentExpression;
import net.mehvahdjukaar.polytone.misc.exp.IExpression;
import net.minecraft.core.BlockPos;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.jetbrains.annotations.NotNull;

record LightmapContextExpression(ConcurrentExpression expression, String unparsed,
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
        return new LightmapContextExpression(createExpression(s),
                s, s.contains(TEMPERATURE) || s.contains(DOWNFALL));
    }

    private static @NotNull ConcurrentExpression createExpression(String s) {
        return ConcurrentExpression.of(
                new ExpressionBuilder(ExpressionUtils.removeHex(s))
                        .variables(TIME, RAIN, THUNDER, DOWNFALL, TEMPERATURE)
                        .functions(ExpressionUtils.defFunc())
                        .operator(ExpressionUtils.defOp())
        );
    }

    @Override
    public double getValue(float time, float rain, float thunder) {
        IExpression.IVars vb = expression.varBuilder();
        vb.setVariable(TIME, time);
        vb.setVariable(RAIN, rain);
        vb.setVariable(THUNDER, thunder);
        BlockPos pos = ClientFrameTicker.getCameraPos();
        vb.setVariable(POS_X, pos.getX());
        vb.setVariable(POS_Y, pos.getY());
        vb.setVariable(POS_Z, pos.getZ());
        if (usesBiome) {
            var biome = ClientFrameTicker.getCameraBiome();
            if (biome == null) {
                vb.setVariable(TEMPERATURE, 0);
                vb.setVariable(DOWNFALL, 0);
            } else {
                var cs = ColorUtils.getClimateSettings(biome.value());
                vb.setVariable(TEMPERATURE, cs.temperature);
                vb.setVariable(DOWNFALL, cs.downfall);
            }

        }
        return expression.evaluate(vb);
    }
}
