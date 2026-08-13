package net.mehvahdjukaar.polytone.companion;

import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

// One texture a piece of content expects (or can use), in the abstract: pure file names, no IO.
// acceptedNames are the names that can fill it, canonical first - tint 0 of a block modifier accepts
// foo_0.png but falls back to foo.png. Bound slots (filling/fillingRemote) are required, unbound
// ones aren't: the reload auto attaches a default colormap when their texture happens to exist.
public record TextureSlot(List<String> acceptedNames, String label,
                          @Nullable Colormap target, @Nullable ResourceLocation remoteLocation) {

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

    public static TextureSlot fillingRemote(Colormap target, ResourceLocation remoteLocation,
                                            String label, String... acceptedNames) {
        return new TextureSlot(List.of(acceptedNames), label, target, remoteLocation);
    }

    public boolean required() {
        return target != null;
    }

    public String canonicalName() {
        return acceptedNames.getFirst();
    }

    // tries each accepted name in order; shared by the reload driver and the editor view
    public <T> @Nullable T findFirstMatch(Function<String, @Nullable T> lookup) {
        for (String fileName : acceptedNames) {
            T found = lookup.apply(fileName);
            if (found != null) return found;
        }
        return null;
    }
}
