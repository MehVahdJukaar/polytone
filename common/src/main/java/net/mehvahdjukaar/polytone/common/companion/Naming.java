package net.mehvahdjukaar.polytone.common.companion;

import net.mehvahdjukaar.polytone.common.StrUtils;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * How a {@link TexturePart}'s files are named relative to its content's stem, plus the shared
 * name math. Two schemes: a literal {@link Suffix} ({@code <stem><suffix>.png}) and the
 * open-ended {@link Tinted} family ({@code <stem>.png} is the default texture, index
 * {@value #DEFAULT_INDEX}, and {@code <stem>_<n>.png} the texture for tint index {@code n}).
 * Everything scheme-dependent is an instance method here, so the engine never branches on the
 * type; the statics are THE single parser/printer for the convention - reload driver and editor
 * both go through them, so they can never disagree (the old regex and hand-rolled copies did:
 * stems containing digits failed the regex and fell out of grouping). All matching is pure and
 * case-insensitive.
 */
public sealed interface Naming {

    int DEFAULT_INDEX = -1;

    /** A base file name split into the content stem and the tint index it encodes. */
    record ParsedName(String stem, int index) {
    }

    // -------------------- per-scheme behavior --------------------

    /** Canonical file name for this part at {@code index} ({@link #DEFAULT_INDEX} = the plain one). */
    String fileName(String stem, int index);

    /** The index {@code fileName} encodes for content {@code stem}, or null when the name isn't this part's. */
    @Nullable Integer indexOf(String fileName, String stem);

    /**
     * Reverse parse with no stem known upfront (orphan routing): the (stem, index) this base
     * name (no extension) encodes, or null when the name can't belong to this part.
     */
    @Nullable ParsedName parseName(String baseName);

    /** Display label for this part's slot at {@code index}; {@code partLabel} is the part's own label. */
    String slotLabel(String partLabel, int index);

    /** The indexes for which {@code textures} has a file of this part under {@code contentId}. */
    Set<Integer> presentIndexes(TrackedTextures textures, Identifier contentId);

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
            return tintedFileName(stem + suffix, index);
        }

        @Override
        public @Nullable Integer indexOf(String fileName, String stem) {
            return fileName.equalsIgnoreCase(stem + suffix + ".png") ? DEFAULT_INDEX : null;
        }

        @Override
        public @Nullable ParsedName parseName(String baseName) {
            if (suffix.isEmpty()) return new ParsedName(baseName, DEFAULT_INDEX);
            if (baseName.length() > suffix.length() && baseName.endsWith(suffix)) {
                return new ParsedName(baseName.substring(0, baseName.length() - suffix.length()), DEFAULT_INDEX);
            }
            return null;
        }

        @Override
        public String slotLabel(String partLabel, int index) {
            return index == DEFAULT_INDEX ? partLabel : partLabel + " " + index;
        }

        @Override
        public Set<Integer> presentIndexes(TrackedTextures textures, Identifier contentId) {
            String stem = StrUtils.lastSegment(contentId.getPath());
            return textures.find(contentId, fileName(stem, DEFAULT_INDEX)) != null
                    ? Set.of(DEFAULT_INDEX) : Set.of();
        }

        @Override
        public int parseSpecificity() {
            return suffix.isEmpty() ? -1 : suffix.length();
        }

        // "" -> "default", "_terrain_fog" -> "terrain fog"
        String derivedLabel() {
            if (suffix.isEmpty()) return label(DEFAULT_INDEX);
            String stripped = suffix.startsWith("_") ? suffix.substring(1) : suffix;
            return stripped.replace('_', ' ');
        }
    }

    record Tinted() implements Naming {

        private static final Tinted INSTANCE = new Tinted();

        @Override
        public String fileName(String stem, int index) {
            return tintedFileName(stem, index);
        }

        @Override
        public @Nullable Integer indexOf(String fileName, String stem) {
            return tintIndexOf(fileName, stem);
        }

        @Override
        public ParsedName parseName(String baseName) {
            return parse(baseName);
        }

        @Override
        public String slotLabel(String partLabel, int index) {
            return label(index);
        }

        @Override
        public Set<Integer> presentIndexes(TrackedTextures textures, Identifier contentId) {
            String dir = StrUtils.directoryOf(contentId.getPath());
            String stem = StrUtils.lastSegment(contentId.getPath());
            Set<Integer> out = new TreeSet<>();
            for (Identifier id : textures.keySet()) {
                if (!id.getNamespace().equals(contentId.getNamespace())) continue;
                String path = id.getPath();
                if (!StrUtils.directoryOf(path).equals(dir)) continue;
                Integer index = tintIndexOf(StrUtils.lastSegment(path) + ".png", stem);
                if (index != null) out.add(index);
            }
            return out;
        }

        @Override
        public int parseSpecificity() {
            return 0;
        }
    }

    // -------------------- shared name math --------------------

    static ParsedName parse(String name) {
        int us = name.lastIndexOf('_');
        if (us > 0) { // us == 0 would leave an empty stem - not a suffix then
            String digits = name.substring(us + 1);
            if (!digits.isEmpty() && digits.length() <= 9 && digits.chars().allMatch(Character::isDigit)) {
                return new ParsedName(name.substring(0, us), Integer.parseInt(digits));
            }
        }
        return new ParsedName(name, DEFAULT_INDEX);
    }

    /**
     * The tint index {@code fileName} (simple name, with extension) encodes for {@code stem},
     * or null when it is not one of that stem's textures. Stem matching is exact and
     * case-insensitive: {@code foobar_1.png} does not match stem {@code foo}. A file whose
     * whole base equals the stem is always the default, even if the stem itself ends in a
     * tint-like suffix ({@code foo_3.png} IS the default texture of a colormap named
     * {@code foo_3}).
     */
    static @Nullable Integer tintIndexOf(String fileName, String stem) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".png")) return null;
        String base = lower.substring(0, lower.length() - ".png".length());
        String lowerStem = stem.toLowerCase(Locale.ROOT);
        if (base.equals(lowerStem)) return DEFAULT_INDEX;
        ParsedName parsed = parse(base);
        return parsed.stem().equals(lowerStem) ? parsed.index() : null;
    }

    static String tintedFileName(String stem, int index) {
        return index == DEFAULT_INDEX ? stem + ".png" : stem + "_" + index + ".png";
    }

    static String label(int index) {
        return index == DEFAULT_INDEX ? "default" : "tint " + index;
    }
}
