package net.mehvahdjukaar.polytone.common.expressions.impl;

import hollowpoint.nexp.api.ExpProgram;
import net.mehvahdjukaar.polytone.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.content.lightmap.ILightmapNumberProvider;

public class LightmapExp extends PolyExp implements ILightmapNumberProvider {

    public static final PolyExpType<LightmapExp> TYPE = new PolyExpType<>(LightmapExp::new,
            c -> c.input(double.class, "time").input(double.class, "rain").input(double.class, "thunder"));

    protected LightmapExp(ExpProgram program, String source) {
        super(program, source);
    }

    @Override
    public double getValue(float time, float rain, float thunder) {
        return executeDouble(time, rain, thunder);
    }
}
