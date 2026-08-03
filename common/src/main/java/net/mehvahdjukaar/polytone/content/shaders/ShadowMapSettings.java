package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Resolved shadow-map parameters queried by {@link ShadowMapRenderer}. Packs author these in
 * polytone/shadow_map.json; absent fields decode to their default, and {@link #merge(ShadowMapSettings)}
 * layers one file over another treating any still-default field as unset, so a pack overrides just the
 * params it actually changed.
 *
 * @param coverage       half-width of the orthographic coverage box, in blocks (the shadowed radius
 *                       around the camera). Smaller = sharper shadows over a smaller area at the same
 *                       resolution.
 * @param depthRange     half-depth of the ortho box along the light axis, in blocks (how far occluders
 *                       above/below still register).
 * @param resolution     shadow map texture size (square), in pixels. Higher = crisper but more VRAM.
 * @param updateInterval minimum ticks between shadow-map re-renders; the map is reused (and re-aligned
 *                       to camera movement) in between. 0 = every frame. Higher trades update latency
 *                       for performance (e.g. 2 ~= 10 updates/s, since a tick is 50 ms).
 * @param renderEntities whether entities cast shadows. Entity models are rebuilt on the CPU for every
 *                       shadow render, so turning this off is the single biggest saving available to a
 *                       pack that only wants terrain shadows.
 * @param renderBlockEntities whether block entities (chests, banners, signs) cast shadows. Same cost
 *                       shape as entities, and usually far less noticeable.
 */
public record ShadowMapSettings(float coverage, float depthRange, int resolution, float updateInterval,
                                boolean renderEntities, boolean renderBlockEntities) {

    public static final ShadowMapSettings DEFAULT = new ShadowMapSettings(64f, 256f, 2048, 0f, true, true);

    public static final Codec<ShadowMapSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("coverage", DEFAULT.coverage).forGetter(ShadowMapSettings::coverage),
            Codec.FLOAT.optionalFieldOf("depth_range", DEFAULT.depthRange).forGetter(ShadowMapSettings::depthRange),
            Codec.INT.optionalFieldOf("resolution", DEFAULT.resolution).forGetter(ShadowMapSettings::resolution),
            Codec.FLOAT.optionalFieldOf("update_interval", DEFAULT.updateInterval).forGetter(ShadowMapSettings::updateInterval),
            Codec.BOOL.optionalFieldOf("render_entities", DEFAULT.renderEntities).forGetter(ShadowMapSettings::renderEntities),
            Codec.BOOL.optionalFieldOf("render_block_entities", DEFAULT.renderBlockEntities).forGetter(ShadowMapSettings::renderBlockEntities)
    ).apply(i, ShadowMapSettings::new));

    public ShadowMapSettings merge(ShadowMapSettings other) {
        return new ShadowMapSettings(
                other.coverage != DEFAULT.coverage ? other.coverage : coverage,
                other.depthRange != DEFAULT.depthRange ? other.depthRange : depthRange,
                other.resolution != DEFAULT.resolution ? other.resolution : resolution,
                other.updateInterval != DEFAULT.updateInterval ? other.updateInterval : updateInterval,
                other.renderEntities != DEFAULT.renderEntities ? other.renderEntities : renderEntities,
                other.renderBlockEntities != DEFAULT.renderBlockEntities ? other.renderBlockEntities : renderBlockEntities);
    }
}
