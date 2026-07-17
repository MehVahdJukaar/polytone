package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * @param coverage       half-width of the orthographic coverage box, in blocks (the shadowed radius
 *                       around the camera). Smaller = sharper shadows over a smaller area at the same
 *                       resolution.
 * @param depthRange     half-depth of the ortho box along the light axis, in blocks (how far occluders
 *                       above/below still register).
 * @param resolution     shadow map texture size (square), in pixels. Higher = crisper but more VRAM.
 * @param updateInterval minimum ticks between shadow-map re-renders; the map is reused (and re-aligned
 *                       to camera movement) in between. 0 = every frame. Higher trades update latency
 *                       for performance (e.g. 2 ~= 10 updates/s, since a tick is 50 ms).
 */
public record ShadowMapSettings(float coverage, float depthRange, int resolution, float updateInterval) {

    public static final ShadowMapSettings DEFAULT = new ShadowMapSettings(64f, 256f, 2048, 0f);

    public static final Codec<ShadowMapSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("coverage", DEFAULT.coverage).forGetter(ShadowMapSettings::coverage),
            Codec.FLOAT.optionalFieldOf("depth_range", DEFAULT.depthRange).forGetter(ShadowMapSettings::depthRange),
            Codec.INT.optionalFieldOf("resolution", DEFAULT.resolution).forGetter(ShadowMapSettings::resolution),
            Codec.FLOAT.optionalFieldOf("update_interval", DEFAULT.updateInterval).forGetter(ShadowMapSettings::updateInterval)
    ).apply(i, ShadowMapSettings::new));
}
