package net.mehvahdjukaar.polytone.bedrock;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.bedrock.convert.BedrockParticleConverter;
import net.mehvahdjukaar.polytone.bedrock.convert.ConversionOptions;
import net.mehvahdjukaar.polytone.bedrock.convert.ConversionResult;
import net.mehvahdjukaar.polytone.bedrock.model.BedrockParticleFile;

import java.util.List;

/**
 * Entry point of the Bedrock particle importer. Nothing here runs during a resource reload: it is meant
 * to be driven by an editor action or a dev command that writes {@link ConversionResult#files()} into a
 * pack.
 */
public class BedrockParticleImporter {

    public static DataResult<BedrockParticleFile> parse(JsonElement json) {
        return BedrockParticleFile.CODEC.parse(JsonOps.INSTANCE, json);
    }

    /** Converts with options taken from the effect's own identifier. */
    public static DataResult<ConversionResult> convert(JsonElement json) {
        return parse(json).map(file -> BedrockParticleConverter.convert(file,
                ConversionOptions.from(file.effect().description())));
    }

    public static DataResult<ConversionResult> convert(JsonElement json, ConversionOptions options) {
        return parse(json).map(file -> BedrockParticleConverter.convert(file, options));
    }

    /** Convenience for logging a whole conversion in one go. */
    public static String summarize(ConversionResult result) {
        StringBuilder out = new StringBuilder("Converted ").append(result.particleId());
        if (result.emitterId() != null) out.append(" (+ emitter ").append(result.emitterId()).append(")");
        out.append(": ").append(result.files().size()).append(" file(s)");
        List<Diagnostic> diagnostics = result.diagnostics();
        if (!diagnostics.isEmpty()) {
            out.append(", ").append(diagnostics.size()).append(" note(s):");
            for (Diagnostic diagnostic : diagnostics) out.append("\n  ").append(diagnostic);
        }
        return out.toString();
    }
}
