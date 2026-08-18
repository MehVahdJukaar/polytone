package net.mehvahdjukaar.polytone.bedrock.convert;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.bedrock.Diagnostic;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// textures stay as requests: cropping the atlas is up to the caller, which is the side that knows where the source pack is
public record ConversionResult(String particleId, @Nullable String emitterId, List<OutputFile> files,
                               List<TextureRequest> textures, List<Diagnostic> diagnostics) {

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.level() == Diagnostic.Level.ERROR);
    }

    // path is pack-relative, e.g. assets/foo/polytone/custom_particles/bar.json
    public record OutputFile(String path, JsonElement content) {
    }

    // A rect of a Bedrock atlas to cut into a standalone sprite. Units are whatever the effect declared:
    // texture_width/height of 1 means the rect is normalised, anything else means pixels. width -1 means whole texture.
    public record TextureRequest(String sourceTexture, String targetSprite, double u, double v,
                                 double width, double height, double textureWidth, double textureHeight) {

        public static TextureRequest wholeTexture(String sourceTexture, String targetSprite) {
            return new TextureRequest(sourceTexture, targetSprite, 0, 0, -1, -1, 1, 1);
        }

        public boolean isWholeTexture() {
            return width < 0 || height < 0;
        }

        public boolean isNormalized() {
            return textureWidth <= 1 && textureHeight <= 1;
        }
    }
}
