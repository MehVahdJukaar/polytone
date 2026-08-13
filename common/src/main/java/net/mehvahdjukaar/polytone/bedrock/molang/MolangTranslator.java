package net.mehvahdjukaar.polytone.bedrock.molang;

import net.mehvahdjukaar.polytone.bedrock.Diagnostic;
import net.mehvahdjukaar.polytone.bedrock.DiagnosticSink;

// Turns Molang source into a Polytone expression. Split out from the converter because the two halves of this
// job are independent: the structural mapping (which component becomes which json field) is settled, the
// language mapping is not.
@FunctionalInterface
public interface MolangTranslator {

    // Copies expressions through untouched and flags anything that isn't already a plain number. Enough to get
    // the skeleton of a conversion right; the result needs hand-editing wherever it warns.
    MolangTranslator PASSTHROUGH = (expr, scope, where, sink) -> {
        if (!expr.isConstant()) {
            sink.warn(where, "Molang '" + Diagnostic.brief(expr.source()) + "' copied verbatim, it needs manual editing");
        }
        return expr.source();
    };

    String translate(MolangExpr expr, Scope scope, String where, DiagnosticSink sink);

    enum Scope {
        // Evaluated by the emitter, which on our side is the meta particle doing the spawning
        EMITTER,
        // Evaluated by an individual particle
        PARTICLE
    }
}
