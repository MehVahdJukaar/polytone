package net.mehvahdjukaar.polytone.compat.nautilus.bedrock;

import java.util.List;

@FunctionalInterface
public interface DiagnosticSink {

    DiagnosticSink IGNORING = d -> {
    };

    void accept(Diagnostic diagnostic);

    default void info(String where, String message) {
        accept(Diagnostic.info(where, message));
    }

    default void warn(String where, String message) {
        accept(Diagnostic.warn(where, message));
    }

    default void error(String where, String message) {
        accept(Diagnostic.error(where, message));
    }

    static DiagnosticSink collectingInto(List<Diagnostic> out) {
        return out::add;
    }
}
