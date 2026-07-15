package net.mehvahdjukaar.polytone.content.color;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

// Render type a painting is drawn with. Default (null in ColorManager) keeps vanilla solid.
public enum PaintingRenderType {
    SOLID,
    CUTOUT,
    TRANSLUCENT;

    public RenderType create(ResourceLocation atlasLocation) {
        return switch (this) {
            case SOLID -> RenderType.entitySolid(atlasLocation);
            case CUTOUT -> RenderType.entityCutout(atlasLocation);
            case TRANSLUCENT -> RenderType.entityTranslucent(atlasLocation);
        };
    }

    @Nullable
    public static PaintingRenderType byName(String name) {
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
