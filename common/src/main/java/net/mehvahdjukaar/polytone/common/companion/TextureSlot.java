package net.mehvahdjukaar.polytone.common.companion;

import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public record TextureSlot(List<String> acceptedNames, String displayLabel,
                          @Nullable Colormap colormapToFill, @Nullable ResourceLocation explicitTexturePath) {

    public TextureSlot {
        if (acceptedNames.isEmpty()) {
            throw new IllegalArgumentException("A texture slot needs at least one accepted file name");
        }
        if (explicitTexturePath != null && colormapToFill == null) {
            throw new IllegalArgumentException("A remote slot must be bound to a colormap");
        }
        acceptedNames = List.copyOf(acceptedNames);
    }

    public static TextureSlot optional(String label, String... acceptedNames) {
        return new TextureSlot(List.of(acceptedNames), label, null, null);
    }

    public static TextureSlot required(Colormap target, String label, String... acceptedNames) {
        return new TextureSlot(List.of(acceptedNames), label, target, null);
    }

    public static TextureSlot requiredExplicit(Colormap target, ResourceLocation explicitTexture,
                                               String label, String... acceptedNames) {
        return new TextureSlot(List.of(acceptedNames), label, target, explicitTexture);
    }

    public boolean required() {
        return colormapToFill != null;
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
