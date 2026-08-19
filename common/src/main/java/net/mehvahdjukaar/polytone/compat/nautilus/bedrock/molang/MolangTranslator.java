package net.mehvahdjukaar.polytone.compat.nautilus.bedrock.molang;

import net.mehvahdjukaar.polytone.compat.nautilus.bedrock.Diagnostic;
import net.mehvahdjukaar.polytone.compat.nautilus.bedrock.DiagnosticSink;

@FunctionalInterface
public interface MolangTranslator {

    // copies expressions through untouched and warns on anything that isn't already a plain number
    MolangTranslator PASSTHROUGH = (expr, scope, where, sink) -> {
        if (!expr.isConstant()) {
            sink.warn(where, "Molang '" + Diagnostic.brief(expr.source()) + "' copied verbatim, it needs manual editing");
        }
        return expr.source();
    };

    String translate(MolangExpr expr, Scope scope, String where, DiagnosticSink sink);

    enum Scope {
        // the emitter is the meta particle doing the spawning on our side
        EMITTER,
        PARTICLE
    }
}
