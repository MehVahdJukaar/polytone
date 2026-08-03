package net.mehvahdjukaar.polytone.compat.nautilus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.nautilus.NautilusStudioApi;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiICons;
import net.mehvahdjukaar.nautilus.workbench.ImportEntry;
import net.mehvahdjukaar.nautilus.workbench.ImportReport;
import net.mehvahdjukaar.nautilus.workbench.PackWorkspace;
import net.mehvahdjukaar.polytone.bedrock.BedrockParticleImporter;
import net.mehvahdjukaar.polytone.bedrock.Diagnostic;
import net.mehvahdjukaar.polytone.bedrock.convert.ConversionOptions;
import net.mehvahdjukaar.polytone.bedrock.convert.ConversionResult;
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

/**
 * Wires the Bedrock particle importer into Nautilus Studio's Import button: one for a single effect,
 * one for a whole Bedrock resource pack. Both write straight into the open workspace and hand back
 * the converter's diagnostics as notes, which is where the user finds out what still needs doing by
 * hand.
 *
 * <p>Existing files are never overwritten - a re-import after hand editing would otherwise throw the
 * edits away. Skipped files are reported so the user can delete and retry deliberately.
 */
public final class BedrockImports {

    // Not html-escaped: expressions are full of < and >, and < in a pack file is unreadable.
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

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
        Writer writer = new Writer(workspace, bedrockRootOf(file));
        writer.convert(file);
        return writer.finish();
    }

    private static ImportReport importPack(PackWorkspace workspace, Path packRoot) throws IOException {
        Path particles = packRoot.resolve(BEDROCK_PARTICLES_DIR);
        if (!Files.isDirectory(particles)) {
            return new ImportReport(List.of(), List.of(ImportReport.Note.error(
                    String.valueOf(packRoot.getFileName()),
                    "No 'particles' folder here. Pick the root of a Bedrock resource pack, the one "
                            + "holding manifest.json")));
        }
        Writer writer = new Writer(workspace, packRoot);
        List<Path> sources = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(particles)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> String.valueOf(p.getFileName()).endsWith(".json"))
                    .sorted()
                    .forEach(sources::add);
        }
        for (Path source : sources) {
            writer.convert(source);
        }
        return writer.finish();
    }

    /**
     * A Bedrock texture path is relative to the resource pack root, so resolving one means knowing
     * where that root is. For a single file it is the parent of the {@code particles} folder it sits
     * in; anything else and textures simply don't resolve, which the report says.
     */
    private static @Nullable Path bedrockRootOf(Path particleFile) {
        Path dir = particleFile.getParent();
        while (dir != null) {
            if (BEDROCK_PARTICLES_DIR.equals(String.valueOf(dir.getFileName()))) return dir.getParent();
            dir = dir.getParent();
        }
        return null;
    }

    /** Accumulates one import: converts effects, writes files, collects notes. */
    private static final class Writer {

        private final PackWorkspace workspace;
        private final @Nullable Path bedrockRoot;
        private final List<Path> written = new ArrayList<>();
        private final List<ImportReport.Note> notes = new ArrayList<>();
        private @Nullable Path firstParticle;

        Writer(PackWorkspace workspace, @Nullable Path bedrockRoot) {
            this.workspace = workspace;
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
                notes.add(ImportReport.Note.error(name, "Could not read the file: " + e.getMessage()));
                return;
            }

            DataResult<ConversionResult> converted = BedrockParticleImporter.convert(json);
            if (converted.result().isEmpty()) {
                notes.add(ImportReport.Note.error(name, "Not a bedrock particle file: "
                        + converted.error().map(DataResult.Error::message).orElse("unknown")));
                return;
            }
            ConversionResult result = converted.result().get();

            for (ConversionResult.OutputFile out : result.files()) {
                Path target = workspace.root().resolve(out.path());
                if (writeJson(target, out.content(), name)) {
                    if (firstParticle == null && out.path().contains("custom_particles")) firstParticle = target;
                }
            }
            for (ConversionResult.TextureRequest texture : result.textures()) {
                extractTexture(texture, name);
            }
            for (Diagnostic diagnostic : result.diagnostics()) {
                notes.add(new ImportReport.Note(severity(diagnostic.level()),
                        name + " · " + diagnostic.where(), diagnostic.message()));
            }
        }

        private boolean writeJson(Path target, JsonElement content, String source) {
            if (Files.exists(target)) {
                notes.add(ImportReport.Note.warn(source, "Kept the existing "
                        + workspace.relativize(target) + "; delete it first to import over it"));
                return false;
            }
            try {
                Files.createDirectories(target.getParent());
                Files.writeString(target, GSON.toJson(content) + "\n");
                written.add(target);
                return true;
            } catch (IOException e) {
                notes.add(ImportReport.Note.error(source, "Could not write "
                        + workspace.relativize(target) + ": " + e.getMessage()));
                return false;
            }
        }

        /** Cuts the effect's rect out of the Bedrock atlas and drops it in as a particle sprite. */
        private void extractTexture(ConversionResult.TextureRequest request, String source) {
            Path target = spritePath(request.targetSprite());
            if (target == null) {
                notes.add(ImportReport.Note.warn(source, "Could not tell where sprite '"
                        + request.targetSprite() + "' should go; add the png yourself"));
                return;
            }
            if (Files.exists(target)) return; // same rule as the json: never clobber
            Path atlas = resolveAtlas(request.sourceTexture());
            if (atlas == null) {
                notes.add(ImportReport.Note.warn(source, "Texture '" + request.sourceTexture()
                        + "' not found in the source pack; add " + workspace.relativize(target) + " yourself"));
                return;
            }
            try {
                BufferedImage image = ImageIO.read(atlas.toFile());
                if (image == null) {
                    notes.add(ImportReport.Note.warn(source, "Could not read " + atlas.getFileName()));
                    return;
                }
                BufferedImage sprite = crop(image, request);
                Files.createDirectories(target.getParent());
                ImageIO.write(sprite, "png", target.toFile());
                written.add(target);
            } catch (IOException e) {
                notes.add(ImportReport.Note.error(source, "Could not extract the sprite: " + e.getMessage()));
            }
        }

        /**
         * Bedrock states the rect against a declared texture size, which need not be the png's real
         * one (and is 1x1 when the rect is a fraction), so everything goes through that ratio.
         */
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

        /** {@code ns:path} to {@code <root>/assets/<ns>/textures/particle/<path>.png}. */
        private @Nullable Path spritePath(String sprite) {
            int colon = sprite.indexOf(':');
            if (colon < 0) return null;
            return workspace.root().resolve("assets").resolve(sprite.substring(0, colon))
                    .resolve("textures/particle").resolve(sprite.substring(colon + 1) + ".png");
        }

        /** Bedrock writes texture paths without an extension, relative to the pack root. */
        private @Nullable Path resolveAtlas(String texture) {
            if (bedrockRoot == null) return null;
            for (String extension : new String[]{".png", ".tga", ""}) {
                Path candidate = bedrockRoot.resolve(texture + extension);
                if (Files.isRegularFile(candidate)) return candidate;
            }
            return null;
        }

        ImportReport finish() {
            return new ImportReport(List.copyOf(written), List.copyOf(notes), firstParticle);
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
