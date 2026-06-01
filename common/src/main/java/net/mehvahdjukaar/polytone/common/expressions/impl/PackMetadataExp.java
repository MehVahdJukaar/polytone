package net.mehvahdjukaar.polytone.common.expressions.impl;

import net.mehvahdjukaar.polytone.common.expressions.ExpUtils;
import net.mehvahdjukaar.polytone.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.common.expressions.PolyExpType;
import org.mvel2.MVEL;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class PackMetadataExp extends PolyExp implements IPackMetadataExp {

    public static final PolyExpType<PackMetadataExp> TYPE =
            new PolyExpType<>(
                    PackMetadataExp::new,
                    ExpUtils::addCommonInputs
            );

    protected PackMetadataExp(Serializable expr, String originalString) {
        super(expr);
    }

    @Override
    public boolean evaluate() {
        Map<String, Object> vars = new HashMap<>();
        ExpUtils.addCommonVars(vars);
        return MVEL.executeExpression(expr, vars, boolean.class);
    }


}
