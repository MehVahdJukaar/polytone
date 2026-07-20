package net.mehvahdjukaar.polytone.common;

/**
 * Small string-formatting helpers shared across the mod. Path/segment math lives in {@link PathsUtils};
 * this is for the rest (class-name display, pluralisation).
 */
public final class StrUtils {

    /**
     * Innermost simple name of a fully-qualified class name ({@code "a.b.Outer$Inner"} -> {@code "Inner"}),
     * matching {@link Class#getSimpleName()} - which is what the slot/widget modifiers match against, so
     * captions show the same name the user would author.
     */
    public static String simpleName(String className) {
        int dot = className.lastIndexOf('.');
        String s = dot >= 0 ? className.substring(dot + 1) : className;
        int inner = s.lastIndexOf('$');
        return inner >= 0 ? s.substring(inner + 1) : s;
    }

    /** {@code "1 slot"}, {@code "2 slots"} - naive English pluralisation. */
    public static String plural(int n, String noun) {
        return n + " " + noun + (n == 1 ? "" : "s");
    }
}
