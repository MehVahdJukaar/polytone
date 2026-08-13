package net.mehvahdjukaar.polytone.bedrock.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// Identity and render setup of an effect. material stays a raw string on purpose: packs in the wild use
// materials outside the documented three, and an unknown one should downgrade to a warning at conversion time
// rather than fail the whole parse.
public record BedrockDescription(String identifier, RenderParams renderParams) {

    public static final BedrockDescription UNNAMED =
            new BedrockDescription("unknown:unnamed", new RenderParams("particles_alpha", ""));

    public static final Codec<BedrockDescription> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("identifier", UNNAMED.identifier).forGetter(BedrockDescription::identifier),
            RenderParams.CODEC.optionalFieldOf("basic_render_parameters", UNNAMED.renderParams)
                    .forGetter(BedrockDescription::renderParams)
    ).apply(i, BedrockDescription::new));

    public String name() {
        int colon = identifier.indexOf(':');
        return colon < 0 ? identifier : identifier.substring(colon + 1);
    }

    public String namespace() {
        int colon = identifier.indexOf(':');
        return colon < 0 ? "" : identifier.substring(0, colon);
    }

    public record RenderParams(String material, String texture) {
        public static final Codec<RenderParams> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("material", "particles_alpha").forGetter(RenderParams::material),
                Codec.STRING.optionalFieldOf("texture", "").forGetter(RenderParams::texture)
        ).apply(i, RenderParams::new));
    }
}
