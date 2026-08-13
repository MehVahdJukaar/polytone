package net.mehvahdjukaar.polytone.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class StrUtils {

    // snake_case or namespaced id to Title Case: "foo:bar_baz" -> "Foo Bar Baz"
    public static String readableName(String name) {
        return Arrays.stream(name.replace(':', '_').split("_"))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(" "));
    }

    // "a/b/c" -> "a/b/", no slash -> ""
    public static String directoryOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash + 1);
    }

    // "a/b/c" -> "c", no slash -> the whole string
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
