package net.mehvahdjukaar.polytone.content.common.expressions.impl;

import hollowpoint.nexp.api.ExpProgram;
import net.mehvahdjukaar.polytone.content.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.content.common.expressions.PolyExpType;

public class PackMetadataExp extends PolyExp implements IPackMetadataExp {

    public static final PolyExpType<PackMetadataExp> TYPE = new PolyExpType<>(PackMetadataExp::new, c -> {});

    protected PackMetadataExp(ExpProgram program, String source) {
        super(program, source);
    }

    @Override
    public boolean evaluate() {
        return executeBool();
    }
}
