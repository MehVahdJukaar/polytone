package net.mehvahdjukaar.polytone.content.common.expressions;

import net.mehvahdjukaar.polytone.Polytone;
import org.mvel2.MVEL;

import java.io.Serializable;
import java.util.Map;

public abstract class PolyExp {
    protected final Serializable expr;
    String unparsed = "";
    private boolean loggedError = false;

    protected PolyExp(Serializable expr) {
        this.expr = expr;
    }

    protected double executeDouble(Map<String, Object> vars) {
        try {
            return toDouble(MVEL.executeExpression(expr, vars));
        } catch (Exception e) {
            logError(e);
            return 0;
        }
    }

    protected boolean executeBool(Map<String, Object> vars) {
        try {
            return toBool(MVEL.executeExpression(expr, vars));
        } catch (Exception e) {
            logError(e);
            return false;
        }
    }

    private void logError(Exception e) {
        if (!loggedError) {
            loggedError = true; // only once per expression so a per-frame failure doesn't spam the log
            Polytone.LOGGER.error("Failed to evaluate expression '{}': {}", unparsed, e.getMessage());
        }
    }

    protected static double toDouble(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof Boolean b) return b ? 1 : 0;
        if (o instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    protected static boolean toBool(Object o) {
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.doubleValue() != 0;
        return o != null;
    }

}
