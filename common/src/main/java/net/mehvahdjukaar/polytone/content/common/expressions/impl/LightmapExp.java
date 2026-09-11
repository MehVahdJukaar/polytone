package net.mehvahdjukaar.polytone.content.common.expressions.impl;

import hollowpoint.nexp.api.ExpProgram;
import net.mehvahdjukaar.polytone.content.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.content.common.expressions.PolyExpType;
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
