package net.mehvahdjukaar.polytone.companion;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * THE parser/printer for the colormap texture naming convention: {@code <stem>.png} is the
 * default texture (index {@value #DEFAULT_INDEX}, applies to all tint indices) and
 * {@code <stem>_<n>.png} the texture for tint index {@code n}. Single source of truth — the
 * runtime association driver ({@code ColormapTextures}) and the editor's companion view both
 * go through {@link #parse}/{@link #tintIndexOf}, so they can never disagree (the old regex
 * and hand-rolled copies did: stems containing digits failed the regex and fell out of
 * grouping).
 */
public final class TintedTextures {

    /** Index meaning "the default texture, applies to all tint indices". */
    public static final int DEFAULT_INDEX = -1;

    /** A name split into owning stem + tint index. */
    public record TintedName(String stem, int index) {
    }

    /**
     * Splits a single path segment (no extension, no directories) into stem + tint index:
     * {@code "foo_3"} → {@code (foo, 3)}; {@code "foo"} / {@code "foo_x"} / {@code "foo_"} →
     * {@code (itself, DEFAULT_INDEX)}. A digit run too long for an int is not a suffix.
     */
    public static TintedName parse(String name) {
        int us = name.lastIndexOf('_');
        if (us > 0) { // us == 0 would leave an empty stem — not a suffix then
            String digits = name.substring(us + 1);
            if (!digits.isEmpty() && digits.length() <= 9 && digits.chars().allMatch(Character::isDigit)) {
                return new TintedName(name.substring(0, us), Integer.parseInt(digits));
            }
        }
        return new TintedName(name, DEFAULT_INDEX);
    }

    /**
     * The tint index {@code fileName} (simple name, with extension) encodes for {@code stem},
     * or null when it is not one of that stem's textures. Stem matching is exact and
     * case-insensitive: {@code foobar_1.png} does not match stem {@code foo}. A file whose
     * whole base equals the stem is always the default, even if the stem itself ends in a
     * tint-like suffix ({@code foo_3.png} IS the default texture of a colormap named
     * {@code foo_3}).
     */
    public static @Nullable Integer tintIndexOf(String fileName, String stem) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".png")) return null;
        String base = lower.substring(0, lower.length() - ".png".length());
        String lowerStem = stem.toLowerCase(Locale.ROOT);
        if (base.equals(lowerStem)) return DEFAULT_INDEX;
        TintedName parsed = parse(base);
        return parsed.stem().equals(lowerStem) ? parsed.index() : null;
    }

    /** Canonical file name for a stem's texture at {@code index}. Inverse of {@link #parse}. */
    public static String fileName(String stem, int index) {
        return index == DEFAULT_INDEX ? stem + ".png" : stem + "_" + index + ".png";
    }

    /** Display label for a tint index: {@code "default"} or {@code "tint <n>"}. */
    public static String label(int index) {
        return index == DEFAULT_INDEX ? "default" : "tint " + index;
    }
}
