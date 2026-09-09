package net.mehvahdjukaar.polytone.common.expressions;

import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;

import java.util.List;

// What MVEL itself can be told to refuse, per context so other MVEL users in the JVM are untouched.
// This alone is NOT a sandbox (getClass() on any value still exists, new/with/loops still parse),
// ExpressionValidator is the real gate. This is the net under it.
public final class MvelLockdown {

    // MVEL keeps these in a static literal table that is consulted before any classloader. Declaring
    // them as inputs of this type makes the compiler resolve a typed variable first, so the class
    // is never reached
    public static final class Shadowed {
    }

    private static final List<String> CLASS_HANDLES = List.of("System", "Class", "ClassLoader", "Runtime", "Thread");

    // SecurityException on purpose: ParseTools.createClass swallows ClassNotFoundException and retries
    // on the thread context loader, anything else propagates as a compile error
    private static final ClassLoader NO_CLASSES = new ClassLoader(null) {
        @Override
        protected Class<?> loadClass(String name, boolean resolve) {
            throw new SecurityException("expressions cannot reference classes: " + name);
        }
    };

    public static ParserContext newContext() {
        ParserConfiguration config = new ParserConfiguration();
        config.setClassLoader(NO_CLASSES);
        ParserContext ctx = new ParserContext(config);
        for (String s : CLASS_HANDLES) {
            ctx.addInput(s, Shadowed.class);
        }
        return ctx;
    }

    public static boolean isShadow(String inputName) {
        return CLASS_HANDLES.contains(inputName);
    }
}
