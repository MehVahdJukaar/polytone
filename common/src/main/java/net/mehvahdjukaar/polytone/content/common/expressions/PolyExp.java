package net.mehvahdjukaar.polytone.content.common.expressions;

import hollowpoint.nexp.api.ExpProgram;
import net.mehvahdjukaar.polytone.Polytone;

public abstract class PolyExp {
    protected final ExpProgram program;
    final String source;
    private boolean loggedError = false;

    protected PolyExp(ExpProgram program, String source) {
        this.program = program;
        this.source = source;
    }

    // inputs go in the order the type declared them
    protected double executeDouble(Object... inputs) {
        try {
            return program.evalDouble(inputs);
        } catch (Exception e) {
            logError(e);
            return 0;
        }
    }

    protected boolean executeBool(Object... inputs) {
        try {
            return program.evalBool(inputs);
        } catch (Exception e) {
            logError(e);
            return false;
        }
    }

    private void logError(Exception e) {
        if (!loggedError) {
            loggedError = true; // only once per expression so a per-frame failure doesn't spam the log
            Polytone.LOGGER.error("Failed to evaluate expression '{}': {}", source, e.getMessage());
        }
    }

}
