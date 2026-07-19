package net.mehvahdjukaar.polytone.utils;

/**
 * Pure segment/extension math on slash-separated resource paths - the strings behind
 * {@link net.minecraft.resources.ResourceLocation#getPath()}. Lives here rather than on
 * Nautilus' FileNamesUtil because reload-time callers run with Nautilus absent (it's not bundled).
 */
public final class PathsUtils {

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

    /**
     * Drops a trailing extension from the last segment only ({@code "a/b/c.png"} -> {@code "a/b/c"}).
     * A dot inside a directory name is left alone, and a path with no extension is returned as-is.
     */
    public static String stripExtension(String path) {
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        return dot > slash ? path.substring(0, dot) : path;
    }
}
