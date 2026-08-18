package net.mehvahdjukaar.polytone.bedrock;

import org.apache.commons.lang3.StringUtils;

// where is the component id or json path the note came from, so the editor can point at it
public record Diagnostic(Level level, String where, String message) {

    public enum Level {
        INFO,
        // converted approximately, or dropped without breaking the result
        WARN,
        // not converted, output is likely wrong
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

    // Molang in the wild runs to thousands of characters; quoting one whole drowns the report
    public static String brief(String text) {
        return StringUtils.abbreviate(StringUtils.normalizeSpace(text), 70);
    }

    @Override
    public String toString() {
        return "[" + level + "] " + where + ": " + message;
    }
}
