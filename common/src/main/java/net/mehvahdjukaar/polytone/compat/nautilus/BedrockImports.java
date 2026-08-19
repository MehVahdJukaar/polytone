package net.mehvahdjukaar.polytone.compat.nautilus;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.nautilus.NautilusStudioApi;
import net.mehvahdjukaar.nautilus.SchemaEditor.Side;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiICons;
import net.mehvahdjukaar.nautilus.workbench.ImportEntry;
import net.mehvahdjukaar.nautilus.workbench.ImportReport;
import net.mehvahdjukaar.nautilus.workbench.ImportSink;
import net.mehvahdjukaar.nautilus.workbench.PackWorkspace;
import net.mehvahdjukaar.polytone.compat.nautilus.bedrock.BedrockParticleImporter;
import net.mehvahdjukaar.polytone.compat.nautilus.bedrock.Diagnostic;
import net.mehvahdjukaar.polytone.compat.nautilus.bedrock.convert.ConversionResult;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

// Wires the Bedrock particle importer into Nautilus Studio's Import button: one entry for a single effect,
// one for a whole resource pack.
public final class BedrockImports {

    private static final String BEDROCK_PARTICLES_DIR = "particles";

    public static void register() {
        NautilusStudioApi.register(new ImportEntry("Bedrock particle", "Polytone",
                ImportEntry.Source.FILE, BedrockImports::importOne)
                .withDescription("Convert one Bedrock .particle.json into a custom particle")
                .withExtensions("json")
                .withIcon(UiICons.content("sparkles")));

        NautilusStudioApi.register(new ImportEntry("Bedrock resource pack", "Polytone",
                ImportEntry.Source.DIRECTORY, BedrockImports::importPack)
                .withDescription("Convert every particle effect in a Bedrock resource pack")
                .withIcon(UiICons.content("package")));
    }

    private static ImportReport importOne(PackWorkspace workspace, Path file) throws IOException {
        Converter converter = new Converter(new ImportSink(workspace), bedrockRootOf(file));
        converter.convert(file);
        return converter.sink.finish();
    }

    private static ImportReport importPack(PackWorkspace workspace, Path packRoot) throws IOException {
        Path particles = packRoot.resolve(BEDROCK_PARTICLES_DIR);
        if (!Files.isDirectory(particles)) {
            return new ImportReport(List.of(), List.of(ImportReport.Note.error(
                    String.valueOf(packRoot.getFileName()),
                    "No 'particles' folder here. Pick the root of a Bedrock resource pack, the one "
                            + "holding manifest.json")));
        }
        Converter converter = new Converter(new ImportSink(workspace), packRoot);
        List<Path> sources = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(particles)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> String.valueOf(p.getFileName()).endsWith(".json"))
                    .sorted()
                    .forEach(sources::add);
        }
        for (Path source : sources) {
            converter.convert(source);
        }
        return converter.sink.finish();
    }

    // A Bedrock texture path is relative to the resource pack root, so resolving one means knowing where that
    // root is. For a single file it is the parent of the particles folder it sits in; anything else and
    // textures simply don't resolve, which the report says.
    private static @Nullable Path bedrockRootOf(Path particleFile) {
        Path dir = particleFile.getParent();
        while (dir != null) {
            if (BEDROCK_PARTICLES_DIR.equals(String.valueOf(dir.getFileName()))) return dir.getParent();
            dir = dir.getParent();
        }
        return null;
    }

    private static final class Converter {

        private final ImportSink sink;
        private final @Nullable Path bedrockRoot;
        private boolean pickedFileToOpen;

        Converter(ImportSink sink, @Nullable Path bedrockRoot) {
            this.sink = sink;
            this.bedrockRoot = bedrockRoot;
        }

        void convert(Path source) {
            String name = String.valueOf(source.getFileName());
            JsonElement json;
            try (Reader reader = Files.newBufferedReader(source)) {
                // lenient on purpose: bedrock json is not strict json, packs in the wild have
                // comments and trailing commas
                json = JsonParser.parseReader(reader);
            } catch (Exception e) {
                sink.note(ImportReport.Note.error(name, "Could not read the file: " + e.getMessage()));
                return;
            }

            DataResult<ConversionResult> converted = BedrockParticleImporter.convert(json);
            if (converted.result().isEmpty()) {
                sink.note(ImportReport.Note.error(name, "Not a bedrock particle file: "
                        + converted.error().map(DataResult.Error::message).orElse("unknown")));
                return;
            }
            ConversionResult result = converted.result().get();

            for (ConversionResult.OutputFile out : result.files()) {
                Path target = sink.workspace().root().resolve(out.path());
                if (sink.writeJson(target, out.content(), name)
                        && !pickedFileToOpen && out.path().contains("custom_particles")) {
                    pickedFileToOpen = true;
                    sink.openAfter(target);
                }
            }
            for (ConversionResult.TextureRequest texture : result.textures()) {
                extractTexture(texture, name);
            }
            for (Diagnostic diagnostic : result.diagnostics()) {
                sink.note(new ImportReport.Note(severity(diagnostic.level()),
                        name + " · " + diagnostic.where(), diagnostic.message()));
            }
        }

        private void extractTexture(ConversionResult.TextureRequest request, String source) {
            Path target = spritePath(request.targetSprite());
            if (target == null) {
                sink.note(ImportReport.Note.warn(source, "Could not tell where sprite '"
                        + request.targetSprite() + "' should go; add the png yourself"));
                return;
            }
            Path atlas = resolveAtlas(request.sourceTexture());
            if (atlas == null) {
                sink.note(ImportReport.Note.warn(source, "Texture '" + request.sourceTexture()
                        + "' not found in the source pack; add "
                        + sink.workspace().relativize(target) + " yourself"));
                return;
            }
            try {
                BufferedImage image = ImageIO.read(atlas.toFile());
                if (image == null) {
                    sink.note(ImportReport.Note.warn(source, "Could not read " + atlas.getFileName()));
                    return;
                }
                sink.writeImage(target, crop(image, request), source);
            } catch (IOException e) {
                sink.note(ImportReport.Note.error(source, "Could not extract the sprite: " + e.getMessage()));
            }
        }

        // Bedrock states the rect against a declared texture size, which need not be the png's real one (and
        // is 1x1 when the rect is a fraction), so everything goes through that ratio.
        private static BufferedImage crop(BufferedImage image, ConversionResult.TextureRequest request) {
            if (request.isWholeTexture()) return image;
            double scaleX = image.getWidth() / Math.max(1e-6, request.textureWidth());
            double scaleY = image.getHeight() / Math.max(1e-6, request.textureHeight());
            int x = clamp((int) Math.round(request.u() * scaleX), 0, image.getWidth() - 1);
            int y = clamp((int) Math.round(request.v() * scaleY), 0, image.getHeight() - 1);
            int w = clamp((int) Math.round(request.width() * scaleX), 1, image.getWidth() - x);
            int h = clamp((int) Math.round(request.height() * scaleY), 1, image.getHeight() - y);
            return image.getSubimage(x, y, w, h);
        }

        private static int clamp(int value, int min, int max) {
            return Math.clamp(value, min, max);
        }

        // ns:path to <root>/assets/<ns>/textures/particle/<path>.png. A bare path has no namespace to
        // place it under, so it is left to the user.
        private @Nullable Path spritePath(String sprite) {
            if (sprite.indexOf(':') < 0) return null;
            Identifier id = Identifier.tryParse(sprite);
            return id == null ? null
                    : sink.workspace().assetPath(Side.CLIENT_RESOURCES, id, "textures/particle", ".png");
        }

        // Bedrock writes texture paths without an extension, relative to the pack root
        private @Nullable Path resolveAtlas(String texture) {
            if (bedrockRoot == null) return null;
            for (String extension : new String[]{".png", ".tga", ""}) {
                Path candidate = bedrockRoot.resolve(texture + extension);
                if (Files.isRegularFile(candidate)) return candidate;
            }
            return null;
        }

        private static ImportReport.Severity severity(Diagnostic.Level level) {
            return switch (level) {
                case ERROR -> ImportReport.Severity.ERROR;
                case WARN -> ImportReport.Severity.WARN;
                case INFO -> ImportReport.Severity.INFO;
            };
        }
    }
}
