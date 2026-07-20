package net.mehvahdjukaar.polytone.common.companion;

import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * One texture a piece of content expects (or can use), in the abstract: pure file names, no IO.
 * {@code acceptedNames} are the file names that can fill the slot, canonical first - e.g. tint 0
 * of a block modifier accepts {@code foo_0.png} but falls back to {@code foo.png}.
 *
 * <p>A slot comes in three shapes: {@link #unbound} (no colormap declared; the reload
 * auto-attaches a default one when the texture exists, so absence is fine), {@link #filling}
 * (bound to the inline {@link Colormap} that receives the texture), and
 * {@link #fillingRemote} (bound, but the file lives at a {@code texture_path} location instead
 * of next to the json). {@link #required()} follows from the shape: bound slots are required,
 * unbound ones never are.</p>
 */
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

    /** A texture the content merely can use by convention: nothing to fill, fine to omit. */
    public static TextureSlot unbound(String label, String... acceptedNames) {
        return new TextureSlot(List.of(acceptedNames), label, null, null);
    }

    /** A texture that gets poured into {@code target} at reload; missing = error. */
    public static TextureSlot filling(Colormap target, String label, String... acceptedNames) {
        return new TextureSlot(List.of(acceptedNames), label, target, null);
    }

    /** Like {@link #filling}, but resolved in {@code remoteLocation}'s directory ({@code texture_path}). */
    public static TextureSlot fillingRemote(Colormap target, Identifier remoteLocation,
                                            String label, String... acceptedNames) {
        return new TextureSlot(List.of(acceptedNames), label, target, remoteLocation);
    }

    /** Bound slots are required (their colormap is empty without the file); unbound ones never are. */
    public boolean required() {
        return target != null;
    }

    public String canonicalName() {
        return acceptedNames.getFirst();
    }

    /**
     * THE slot-matching rule, shared by the reload driver and the editor view so the two can't
     * disagree: try each accepted name in order and return the first candidate {@code lookup}
     * resolves it to (a texture id, a sibling path, ...), or null when none match.
     */
    public <T> @Nullable T findFirstMatch(Function<String, @Nullable T> lookup) {
        for (String fileName : acceptedNames) {
            T found = lookup.apply(fileName);
            if (found != null) return found;
        }
        return null;
    }
}
