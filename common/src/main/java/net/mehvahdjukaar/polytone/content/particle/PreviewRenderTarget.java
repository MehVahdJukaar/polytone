package net.mehvahdjukaar.polytone.content.particle;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.jetbrains.annotations.Nullable;

/**
 * Render-thread hand-off for the custom-particle editor preview: while the preview draws its sandbox
 * particle offscreen, this holds the offscreen {@link RenderTarget} it wants the vanilla particle
 * feature renderer to draw into.
 *
 * <p>{@code ParticleFeatureRenderer} builds its render pass straight from
 * {@code Minecraft#getMainRenderTarget()} (and {@code LevelRenderer#getParticlesTarget()}), ignoring
 * {@code RenderSystem.outputColorTextureOverride}, so the editor's offscreen redirect never reached
 * particles - they drew onto the game screen behind the editor. {@code MinecraftMixin} /
 * {@code LevelRendererMixin} consult this to send those reads to the offscreen buffer instead.
 *
 * <p>Kept in the always-present core package (only vanilla types in its signature) so the mixins can
 * reference it even when the optional Nautilus editor isn't installed. The offscreen target itself is
 * handed in by the Nautilus preview, which owns it.
 */
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
