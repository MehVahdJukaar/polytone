package net.mehvahdjukaar.polytone.bedrock.convert;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.bedrock.Diagnostic;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// Everything one Bedrock effect turned into. Files are pack-relative and ready to write; textures are left as
// requests because cropping an atlas is the caller's job (and the caller is the one that knows where the
// source pack lives).
public record ConversionResult(String particleId, @Nullable String emitterId, List<OutputFile> files,
                               List<TextureRequest> textures, List<Diagnostic> diagnostics) {

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.level() == Diagnostic.Level.ERROR);
    }

    // pack-relative, e.g. assets/foo/polytone/custom_particles/bar.json
    public record OutputFile(String path, JsonElement content) {
    }

    // A rect of a Bedrock texture atlas that needs to become a standalone sprite png. Coordinates are in
    // whatever unit the effect declared: texture_width/texture_height of 1 means the rect is normalised,
    // anything else means pixels. A width of -1 means "take the whole texture".
    public record TextureRequest(String sourceTexture, String targetSprite, double u, double v,
                                 double width, double height, double textureWidth, double textureHeight) {

        public static TextureRequest wholeTexture(String sourceTexture, String targetSprite) {
            return new TextureRequest(sourceTexture, targetSprite, 0, 0, -1, -1, 1, 1);
        }

        public boolean isWholeTexture() {
            return width < 0 || height < 0;
        }

        // True when the rect is expressed as a fraction of the texture rather than in pixels
        public boolean isNormalized() {
            return textureWidth <= 1 && textureHeight <= 1;
        }
    }
}
