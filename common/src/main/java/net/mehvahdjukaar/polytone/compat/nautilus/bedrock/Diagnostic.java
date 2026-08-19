package net.mehvahdjukaar.polytone.compat.nautilus.bedrock;

import org.apache.commons.lang3.StringUtils;

public record Diagnostic(Level level, String where, String message) {

    public enum Level {
        INFO,
        WARN,
        ERROR
    }

    public static Diagnostic info(String where, String message) {
        return new Diagnostic(Level.INFO, where, message);
    }

    public static Diagnostic warn(String where, String message) {
        return new Diagnostic(Level.WARN, where, message);
    }

    public static Diagnostic error(String where, String message) {
        return new Diagnostic(Level.ERROR, where, message);
    }

    public static String brief(String text) {
        return StringUtils.abbreviate(StringUtils.normalizeSpace(text), 70);
    }

    @Override
    public String toString() {
        return "[" + level + "] " + where + ": " + message;
    }
}
