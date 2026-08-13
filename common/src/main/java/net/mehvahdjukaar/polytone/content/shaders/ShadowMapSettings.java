package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// Packs author these in polytone/shadow_map.json. merge() layers one file over another treating any
// still-default field as unset, so a pack overrides just the params it actually changed.
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
