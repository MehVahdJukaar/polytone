package net.mehvahdjukaar.polytone.content.color;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

// Render type a painting is drawn with. Default (null in ColorManager) keeps vanilla solid.
public enum PaintingRenderType {
    SOLID,
    CUTOUT,
    TRANSLUCENT;

    public RenderType create(Identifier atlasLocation) {
        return switch (this) {
            case SOLID -> RenderTypes.entitySolidZOffsetForward(atlasLocation);
            case CUTOUT -> RenderTypes.entityCutout(atlasLocation);
            case TRANSLUCENT -> RenderTypes.entityTranslucent(atlasLocation);
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
