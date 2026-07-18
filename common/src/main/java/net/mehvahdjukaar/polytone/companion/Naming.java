package net.mehvahdjukaar.polytone.companion;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.TreeSet;

/**
 * How a {@link TexturePart}'s files are named relative to its content's stem. Two schemes:
 * a literal {@link Suffix} ({@code <stem><suffix>.png}) and the open-ended {@link Tinted}
 * family ({@code <stem>.png} plus {@code <stem>_<n>.png}, one file per tint index). Everything
 * scheme-dependent lives here - name math (via {@link TintNaming}), slot labeling, and which
 * indexes a scanned texture store has for a content - so the engine never branches on the type.
 */
public sealed interface Naming {

    /** Canonical file name for this part at {@code index} ({@link TintNaming#DEFAULT_INDEX} = the plain one). */
    String fileName(String stem, int index);

    /** The index {@code fileName} encodes for content {@code stem}, or null when the name isn't this part's. */
    @Nullable Integer indexOf(String fileName, String stem);

    /**
     * Reverse parse with no stem known upfront (orphan routing): the (stem, index) this base
     * name (no extension) encodes, or null when the name can't belong to this part.
     */
    TintNaming.@Nullable TintedName parseName(String baseName);

    /** Display label for this part's slot at {@code index}; {@code partLabel} is the part's own label. */
    String slotLabel(String partLabel, int index);

    /** The indexes for which {@code textures} has a file of this part under {@code contentId}. */
    Set<Integer> presentIndexes(TrackedTextures textures, ResourceLocation contentId);

    /** Orphan-routing order: higher parses first (long literal suffixes beat the open tinted family beats ""). */
    int parseSpecificity();

    static Naming suffix(String suffix) {
        return new Suffix(suffix);
    }

    static Naming tinted() {
        return Tinted.INSTANCE;
    }

    record Suffix(String suffix) implements Naming {

        @Override
        public String fileName(String stem, int index) {
            return TintNaming.fileName(stem + suffix, index);
        }

        @Override
        public @Nullable Integer indexOf(String fileName, String stem) {
            return fileName.equalsIgnoreCase(stem + suffix + ".png") ? TintNaming.DEFAULT_INDEX : null;
        }

        @Override
        public TintNaming.@Nullable TintedName parseName(String baseName) {
            if (suffix.isEmpty()) return new TintNaming.TintedName(baseName, TintNaming.DEFAULT_INDEX);
            if (baseName.length() > suffix.length() && baseName.endsWith(suffix)) {
                return new TintNaming.TintedName(baseName.substring(0, baseName.length() - suffix.length()),
                        TintNaming.DEFAULT_INDEX);
            }
            return null;
        }

        @Override
        public String slotLabel(String partLabel, int index) {
            return index == TintNaming.DEFAULT_INDEX ? partLabel : partLabel + " " + index;
        }

        @Override
        public Set<Integer> presentIndexes(TrackedTextures textures, ResourceLocation contentId) {
            String stem = TintNaming.lastSegment(contentId.getPath());
            return textures.find(contentId, fileName(stem, TintNaming.DEFAULT_INDEX)) != null
                    ? Set.of(TintNaming.DEFAULT_INDEX) : Set.of();
        }

        @Override
        public int parseSpecificity() {
            return suffix.isEmpty() ? -1 : suffix.length();
        }

        /** {@code ""} -> "default", {@code "_terrain_fog"} -> "terrain fog" */
        String derivedLabel() {
            if (suffix.isEmpty()) return TintNaming.label(TintNaming.DEFAULT_INDEX);
            String stripped = suffix.startsWith("_") ? suffix.substring(1) : suffix;
            return stripped.replace('_', ' ');
        }
    }

    record Tinted() implements Naming {

        private static final Tinted INSTANCE = new Tinted();

        @Override
        public String fileName(String stem, int index) {
            return TintNaming.fileName(stem, index);
        }

        @Override
        public @Nullable Integer indexOf(String fileName, String stem) {
            return TintNaming.tintIndexOf(fileName, stem);
        }

        @Override
        public TintNaming.TintedName parseName(String baseName) {
            return TintNaming.parse(baseName);
        }

        @Override
        public String slotLabel(String partLabel, int index) {
            return TintNaming.label(index);
        }

        @Override
        public Set<Integer> presentIndexes(TrackedTextures textures, ResourceLocation contentId) {
            String dir = TintNaming.directoryOf(contentId.getPath());
            String stem = TintNaming.lastSegment(contentId.getPath());
            Set<Integer> out = new TreeSet<>();
            for (ResourceLocation id : textures.keySet()) {
                if (!id.getNamespace().equals(contentId.getNamespace())) continue;
                String path = id.getPath();
                if (!TintNaming.directoryOf(path).equals(dir)) continue;
                Integer index = TintNaming.tintIndexOf(TintNaming.lastSegment(path) + ".png", stem);
                if (index != null) out.add(index);
            }
            return out;
        }

        @Override
        public int parseSpecificity() {
            return 0;
        }
    }
}
