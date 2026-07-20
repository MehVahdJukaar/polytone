package net.mehvahdjukaar.polytone.common.expressions.impl;

import net.mehvahdjukaar.polytone.common.expressions.ExpUtils;
import net.mehvahdjukaar.polytone.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.content.lightmap.ILightmapNumberProvider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// MVEL counterpart of the exp4j LightmapContextExpression. The lightmap inputs (celestial time,
// rain, thunder) are bound as bare variables since they differ from g.time()/g.rain() (game time /
// combined weather); everything else is reachable through the common proxies (g, c, p, r).
public class LightmapExp extends PolyExp implements ILightmapNumberProvider {

    public static final PolyExpType<LightmapExp> TYPE =
            new PolyExpType<>(
                    LightmapExp::new,
                    c -> {
                        ExpUtils.addCommonInputs(c);
                        c.addInput("time", double.class);
                        c.addInput("rain", double.class);
                        c.addInput("thunder", double.class);
                    }
            );

    protected LightmapExp(Serializable expr) {
        super(expr);
    }

    @Override
    public double getValue(float time, float rain, float thunder) {
        Map<String, Object> vars = new HashMap<>();
        ExpUtils.addCommonVars(vars);
        vars.put("time", (double) time);
        vars.put("rain", (double) rain);
        vars.put("thunder", (double) thunder);
        return executeDouble(vars);
    }
}
