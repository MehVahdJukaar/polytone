package net.mehvahdjukaar.polytone.bedrock;

import org.apache.commons.lang3.StringUtils;

/**
 * A single note about a conversion. {@code where} is the component id (or json path) the note came
 * from, so the editor can point the user at the thing that didn't survive the trip.
 */
public record Diagnostic(Level level, String where, String message) {

    public enum Level {
        /** Converted, but the author should know how. */
        INFO,
        /** Converted approximately, or dropped without breaking the result. */
        WARN,
        /** Could not be converted; the output is likely wrong or incomplete. */
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

    /** Molang in the wild runs to thousands of characters; quoting one whole drowns the report. */
    public static String brief(String text) {
        return StringUtils.abbreviate(StringUtils.normalizeSpace(text), 70);
    }

    @Override
    public String toString() {
        return "[" + level + "] " + where + ": " + message;
    }
}
