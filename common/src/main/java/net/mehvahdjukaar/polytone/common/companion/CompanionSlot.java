package net.mehvahdjukaar.polytone.common.companion;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One companion file a piece of content expects (or can use), in the abstract: pure names, no
 * IO. {@code acceptedNames} are the file names that can fill the slot, canonical first — e.g.
 * tint 0 of a block modifier accepts {@code foo_0.png} but falls back to {@code foo.png}.
 * {@code required} means the runtime reload errors when the slot stays empty; optional slots
 * are consumed when present (auto-default content) but fine to omit.
 *
 * <p>A slot may be <em>bound</em>: {@code target} is the object that receives the file's
 * content at reload (for textures, the inline {@code Colormap} to fill) — opaque here, used
 * by the runtime driver and ignored by the editor. {@code remoteLocation} marks a slot whose
 * file does NOT live next to the json ({@code texture_path}): a resource location string
 * (extension-less, {@code ns:path}) the accepted names resolve against instead of the json's
 * own directory.</p>
 */
public record CompanionSlot(List<String> acceptedNames, String label, boolean required,
                            @Nullable Object target, @Nullable String remoteLocation) {

    public CompanionSlot {
        if (acceptedNames.isEmpty()) {
            throw new IllegalArgumentException("A companion slot needs at least one accepted file name");
        }
        acceptedNames = List.copyOf(acceptedNames);
    }

    /** The name shown (and expected) when the slot is empty. */
    public String canonicalName() {
        return acceptedNames.getFirst();
    }

    public static CompanionSlot required(String label, String... acceptedNames) {
        return new CompanionSlot(List.of(acceptedNames), label, true, null, null);
    }

    public static CompanionSlot optional(String label, String... acceptedNames) {
        return new CompanionSlot(List.of(acceptedNames), label, false, null, null);
    }
}
