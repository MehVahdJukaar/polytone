package net.mehvahdjukaar.polytone.companion;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * One companion texture family a content type carries, declared on its manager's
 * {@code Spec.textureParts}: how its files are named, the label the editor shows for them, and
 * how to read what the parsed json declares for it. Managers keep these as constants and compare
 * by identity when deciding which default content adopts a lone texture - construction never
 * lives in this package. Declaration order matters: the first part is the content type's main
 * feature and claims plain {@code <stem>.png} files no other part explains.
 */
public record TexturePart<V>(Naming naming, String label,
                             Function<V, @Nullable Object> declaredGetter) {

    /**
     * The raw declared object for this part on a parsed value: an inline {@link
     * net.mehvahdjukaar.polytone.content.colormap.Colormap}, an indexed compound, a
     * reference/expression, or null when nothing is declared.
     */
    public @Nullable Object declared(V value) {
        return declaredGetter.apply(value);
    }

    /** The unsuffixed {@code <stem>.png} part - a content type's single or most important texture. */
    public static <V> TexturePart<V> plain(Function<V, @Nullable Object> declared) {
        return suffix("", declared);
    }

    /** Same, with an explicit label where "default" is wrong (item/fluid tint colormaps). */
    public static <V> TexturePart<V> plain(String label, Function<V, @Nullable Object> declared) {
        return new TexturePart<>(Naming.suffix(""), label, declared);
    }

    /** A {@code <stem><suffix>.png} part; label derived from the suffix ("_terrain_fog" -> "terrain fog"). */
    public static <V> TexturePart<V> suffix(String suffix, Function<V, @Nullable Object> declared) {
        Naming.Suffix naming = new Naming.Suffix(suffix);
        return new TexturePart<>(naming, naming.derivedLabel(), declared);
    }

    /** The open-ended {@code <stem>_<tint>.png} family, one file per tint index (block modifiers). */
    public static <V> TexturePart<V> tinted(Function<V, @Nullable Object> declared) {
        return new TexturePart<>(Naming.tinted(), Naming.label(Naming.DEFAULT_INDEX), declared);
    }
}
