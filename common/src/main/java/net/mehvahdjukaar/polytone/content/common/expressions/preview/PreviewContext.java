package net.mehvahdjukaar.polytone.content.common.expressions.preview;

import org.jetbrains.annotations.Nullable;

public final class PreviewContext {

    private static final ThreadLocal<SimProxies> ACTIVE = new ThreadLocal<>();

    public static @Nullable SimProxies active() {
        return ACTIVE.get();
    }

    public static void install(SimProxies sim) {
        ACTIVE.set(sim);
    }

    public static void clear() {
        ACTIVE.remove();
    }
}
