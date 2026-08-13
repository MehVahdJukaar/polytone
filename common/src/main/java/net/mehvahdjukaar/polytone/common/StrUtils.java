package net.mehvahdjukaar.polytone.common;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class StrUtils {

    public static String readableName(String name) {
        return Arrays.stream(name.replace(':', '_').split("_"))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(" "));
    }

    public static String directoryOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash + 1);
    }

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

    // 3.0 -> "3", 3.5 -> "3.5"; keeps generated json and expressions readable.
    public static String compactNumber(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
