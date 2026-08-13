package net.mehvahdjukaar.polytone.content.particle;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.jetbrains.annotations.Nullable;

// Render-thread hand-off for the custom-particle editor preview: while the preview draws its sandbox particle
// offscreen, this holds the offscreen RenderTarget it wants the vanilla particle feature renderer to draw
// into.
public final class PreviewRenderTarget {

    private static @Nullable RenderTarget current;

    public static void begin(RenderTarget target) {
        current = target;
    }

    public static void end() {
        current = null;
    }

    public static @Nullable RenderTarget current() {
        return current;
    }
}
