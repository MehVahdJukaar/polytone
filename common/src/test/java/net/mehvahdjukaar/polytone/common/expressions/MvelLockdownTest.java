package net.mehvahdjukaar.polytone.common.expressions;

import org.junit.jupiter.api.Test;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// what MVEL itself refuses on a locked context, with no validator in front. Keywords (new, with,
// loops, def) are validator-only and not covered here. Payloads are all benign in case something
// regresses: nothing is ever executed, and they'd only read a name or a count
class MvelLockdownTest {

    static ParserContext ctx() {
        ParserContext ctx = MvelLockdown.newContext();
        ctx.setStrongTyping(true);
        ctx.setStrictTypeEnforcement(true);
        ctx.addInput("x", double.class);
        return ctx;
    }

    static void rejects(String expr) {
        assertThrows(RuntimeException.class, () -> MVEL.compileExpression(expr, ctx()), expr);
    }

    @Test
    void classHandlesAreShadowed() {
        rejects("Runtime.getRuntime().availableProcessors()");
        rejects("System.nanoTime()");
        rejects("Thread.currentThread().getName()");
        rejects("Class.forName('java.lang.String')");
        rejects("ClassLoader.getSystemClassLoader()");
    }

    @Test
    void fullyQualifiedNamesDontResolve() {
        rejects("java.lang.Thread.currentThread().getName()");
        rejects("java.lang.System.nanoTime()");
        rejects("java.util.Collections.emptyList()");
        rejects("new java.util.ArrayList()");
        rejects("import java.util.ArrayList; 1");
        rejects("x instanceof java.lang.Double");
    }

    @Test
    void ordinaryScriptsStillWork() {
        Serializable e = MVEL.compileExpression("t = x * 2; t > 1 ? t + 1 : 0", ctx());
        assertEquals(7.0, ((Number) MVEL.executeExpression(e, new HashMap<>(Map.of("x", 3.0)))).doubleValue());
        Serializable s = MVEL.compileExpression("if (x > 1) { 'big' } else { 'small' }", ctx());
        assertEquals("big", MVEL.executeExpression(s, new HashMap<>(Map.of("x", 3.0))));
    }
}
