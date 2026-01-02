package net.mehvahdjukaar.polytone.common.expressions;

import java.io.Serializable;

public abstract class PolyExp {
    protected final Serializable expr;

    protected PolyExp(Serializable expr) {
        this.expr = expr;
    }

}
