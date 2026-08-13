package net.mehvahdjukaar.polytone.companion;

import net.mehvahdjukaar.polytone.utils.StrUtils;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

// How a TexturePart's files are named relative to its content stem: a literal suffix
// (<stem><suffix>.png) or the tinted family (<stem>.png default, <stem>_<n>.png for tint n).
// The statics are the only parser/printer for it, shared by reload and editor.
public sealed interface Naming {

    int DEFAULT_INDEX = -1;

    record ParsedName(String stem, int index) {
    }

    String fileName(String stem, int index);

    @Nullable Integer indexOf(String fileName, String stem);

    @Nullable ParsedName parseName(String baseName);

    String slotLabel(String partLabel, int index);

    Set<Integer> presentIndexes(TrackedTextures textures, ResourceLocation contentId);

    // orphan routing order: higher parses first (long literal suffixes beat the tinted family beats "")
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
        public Set<Integer> presentIndexes(TrackedTextures textures, ResourceLocation contentId) {
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
        public Set<Integer> presentIndexes(TrackedTextures textures, ResourceLocation contentId) {
            String dir = StrUtils.directoryOf(contentId.getPath());
            String stem = StrUtils.lastSegment(contentId.getPath());
            Set<Integer> out = new TreeSet<>();
            for (ResourceLocation id : textures.keySet()) {
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

    // Stem matching is exact and case insensitive: foobar_1.png does not match stem foo. A file whose
    // whole base equals the stem is always the default, even if the stem itself ends in a tint suffix
    // (foo_3.png IS the default texture of a colormap named foo_3).
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
