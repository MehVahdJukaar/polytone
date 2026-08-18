package net.mehvahdjukaar.polytone.content.particle.gpu;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.mehvahdjukaar.polytone.content.particle.custom.ParticleRenderMode;
import net.mehvahdjukaar.polytone.content.particle.custom.RotationMode;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record GpuParticleType(Identifier texture,
                              Identifier shader,
                              int limit,
                              ParticleRenderMode renderType,
                              RotationMode rotationMode,
                              int lightLevel,
                              GpuParticleInitializer initializer,
                              float gravity,
                              float friction,
                              Optional<Float> sizeEnd,
                              Optional<Integer> colorEnd,
                              Fade fade,
                              float sway,
                              float spin,
                              float aspect,
                              int frames,
                              boolean randomSprite,
                              Map<String, ISimpleExp> uniforms) {

    public static final Identifier DEFAULT_SHADER = Polytone.res("gpu_particle");
    public static final int MAX_LIMIT = 1_000_000;

    public static final String TYPE_KEY = "type";
    public static final String TYPE_VALUE = "gpu";
    // discriminator that tells the custom particle codec to come here, always written back
    private static final Codec<String> TYPE_TAG = Codec.STRING.validate(s -> s.equals(TYPE_VALUE)
            ? DataResult.success(s) : DataResult.error(() -> "expected \"" + TYPE_VALUE + "\""));

    public static final SchemaCodec<GpuParticleType> CODEC = SchemaRecord.create(GpuParticleType.class, i -> i.group(
            i.field(TYPE_KEY, TYPE_TAG, t -> TYPE_VALUE),
            i.field("texture", Identifier.CODEC, GpuParticleType::texture),
            i.optional("shader", Identifier.CODEC, DEFAULT_SHADER, GpuParticleType::shader),
            i.optional("limit", Codec.intRange(1, MAX_LIMIT), 16384, GpuParticleType::limit),
            i.optional("render_type", ParticleRenderMode.CODEC, ParticleRenderMode.OPAQUE, GpuParticleType::renderType),
            i.optional("rotation_mode", RotationMode.CODEC, RotationMode.LOOK_AT_XYZ, GpuParticleType::rotationMode),
            i.optional("light_level", Codec.intRange(0, 15), 0, GpuParticleType::lightLevel),
            i.optional("initializer", GpuParticleInitializer.CODEC, GpuParticleInitializer.DEFAULT, GpuParticleType::initializer),
            i.optional("gravity", Codec.FLOAT, 0f, GpuParticleType::gravity),
            i.optional("friction", Codec.floatRange(0, 1), 1f, GpuParticleType::friction),
            i.optional("size_end", Codec.floatRange(0, Float.MAX_VALUE), GpuParticleType::sizeEnd),
            i.optional("color_end", ColorUtils.COLOR, GpuParticleType::colorEnd),
            i.optional("fade", Fade.CODEC, new Fade(0f, 0f), GpuParticleType::fade),
            i.optional("sway", Codec.FLOAT, 0f, GpuParticleType::sway),
            i.optional("spin", Codec.FLOAT, 0f, GpuParticleType::spin),
            i.optional("aspect", ExtraCodecs.POSITIVE_FLOAT, 1f, GpuParticleType::aspect),
            i.optional("frames", ExtraCodecs.POSITIVE_INT, 1, GpuParticleType::frames),
            i.optional("random_sprite", Codec.BOOL, false, GpuParticleType::randomSprite),
            i.optional("uniforms", Codec.unboundedMap(Codec.STRING, ISimpleExp.CODEC), Map.of(), GpuParticleType::uniforms)
    ).apply(i, (tag, texture, shader, limit, renderType, rotationMode, lightLevel, initializer, gravity, friction,
                sizeEnd, colorEnd, fade, sway, spin, aspect, frames, randomSprite, uniforms) ->
            new GpuParticleType(texture, shader, limit, renderType, rotationMode, lightLevel, initializer, gravity, friction,
                    sizeEnd, colorEnd, fade, sway, spin, aspect, frames, randomSprite, uniforms)));

    public record Fade(float in, float out) {
        public static final Codec<Fade> CODEC = Codec.floatRange(0, 1).listOf(2, 2)
                .xmap(l -> new Fade(l.get(0), l.get(1)), f -> List.of(f.in, f.out));
    }
}
