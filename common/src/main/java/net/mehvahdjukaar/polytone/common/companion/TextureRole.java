package net.mehvahdjukaar.polytone.common.companion;

import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public record TextureRole<V>(FileNamePattern namePattern, String displayLabel,
                             Function<V, @Nullable IColorGetter> declaredColormapGetter) {

    public @Nullable IColorGetter getDeclaredColormap(V value) {
        return declaredColormapGetter.apply(value);
    }

    public static <V> TextureRole<V> plain(Function<V, @Nullable IColorGetter> declared) {
        return suffix("", declared);
    }

    public static <V> TextureRole<V> plain(String label, Function<V, @Nullable IColorGetter> declared) {
        return new TextureRole<>(FileNamePattern.suffix(""), label, declared);
    }

    public static <V> TextureRole<V> suffix(String suffix, Function<V, @Nullable IColorGetter> declared) {
        FileNamePattern.Suffix naming = new FileNamePattern.Suffix(suffix);
        return new TextureRole<>(naming, naming.displayLabel(), declared);
    }

    public static <V> TextureRole<V> tinted(Function<V, @Nullable IColorGetter> declared) {
        return new TextureRole<>(FileNamePattern.tinted(), FileNamePattern.label(FileNamePattern.NO_INDEX), declared);
    }
}
