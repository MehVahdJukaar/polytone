package net.mehvahdjukaar.polytone.common;

public final class StrUtils {

    /** {@code "a/b/c"} -> {@code "a/b/"}; no slash -> {@code ""}. */
    public static String directoryOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash + 1);
    }

    /** {@code "a/b/c"} -> {@code "c"}; no slash -> the whole string. */
    public static String lastSegment(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    public static String stripExtension(String path) {
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        return dot > slash ? path.substring(0, dot) : path;
    }

    public static String simpleName(String className) {
        int dot = className.lastIndexOf('.');
        String s = dot >= 0 ? className.substring(dot + 1) : className;
        int inner = s.lastIndexOf('$');
        return inner >= 0 ? s.substring(inner + 1) : s;
    }

    public static String plural(int n, String noun) {
        return n + " " + noun + (n == 1 ? "" : "s");
    }
}
