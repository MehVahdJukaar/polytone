package net.mehvahdjukaar.polytone.common.companion;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

// One companion texture family a content type carries. Declaration order matters: the first part is
// the main feature and claims plain <stem>.png files no other part explains.
public record TexturePart<V>(Naming naming, String label,
                             Function<V, @Nullable Object> declaredGetter) {

    public @Nullable Object declared(V value) {
        return declaredGetter.apply(value);
    }

    public static <V> TexturePart<V> plain(Function<V, @Nullable Object> declared) {
        return suffix("", declared);
    }

    public static <V> TexturePart<V> plain(String label, Function<V, @Nullable Object> declared) {
        return new TexturePart<>(Naming.suffix(""), label, declared);
    }

    public static <V> TexturePart<V> suffix(String suffix, Function<V, @Nullable Object> declared) {
        Naming.Suffix naming = new Naming.Suffix(suffix);
        return new TexturePart<>(naming, naming.derivedLabel(), declared);
    }

    public static <V> TexturePart<V> tinted(Function<V, @Nullable Object> declared) {
        return new TexturePart<>(Naming.tinted(), Naming.label(Naming.DEFAULT_INDEX), declared);
    }
}
