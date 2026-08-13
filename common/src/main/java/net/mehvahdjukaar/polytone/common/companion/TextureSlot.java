package net.mehvahdjukaar.polytone.common.companion;

import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

// One texture a piece of content expects, in the abstract: pure file names, no IO. acceptedNames are
// the names that can fill it, canonical first. Bound slots are required, unbound ones aren't.
public record TextureSlot(List<String> acceptedNames, String label,
                          @Nullable Colormap target, @Nullable Identifier remoteLocation) {

    public TextureSlot {
        if (acceptedNames.isEmpty()) {
            throw new IllegalArgumentException("A texture slot needs at least one accepted file name");
        }
        if (remoteLocation != null && target == null) {
            throw new IllegalArgumentException("A remote slot must be bound to a colormap");
        }
        acceptedNames = List.copyOf(acceptedNames);
    }

    public static TextureSlot unbound(String label, String... acceptedNames) {
        return new TextureSlot(List.of(acceptedNames), label, null, null);
    }

    public static TextureSlot filling(Colormap target, String label, String... acceptedNames) {
        return new TextureSlot(List.of(acceptedNames), label, target, null);
    }

    public static TextureSlot fillingRemote(Colormap target, Identifier remoteLocation,
                                            String label, String... acceptedNames) {
        return new TextureSlot(List.of(acceptedNames), label, target, remoteLocation);
    }

    public boolean required() {
        return target != null;
    }

    public String canonicalName() {
        return acceptedNames.getFirst();
    }

    public <T> @Nullable T findFirstMatch(Function<String, @Nullable T> lookup) {
        for (String fileName : acceptedNames) {
            T found = lookup.apply(fileName);
            if (found != null) return found;
        }
        return null;
    }
}
