package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public record ExtraDataParticleOptions(Map<String, Float> extraData,
                                       ParticleType<?> type) implements ParticleOptions {
    public static MapCodec<ExtraDataParticleOptions> codec(Supplier<ParticleType<ExtraDataParticleOptions>> typeGetter) {
        return Codec.unboundedMap(Codec.STRING, Codec.FLOAT)
                .optionalFieldOf("extra_data", Map.of())
                .xmap(stringFloatMap -> new ExtraDataParticleOptions(stringFloatMap, typeGetter.get()), ExtraDataParticleOptions::extraData);

    }

    public static StreamCodec<ByteBuf, ExtraDataParticleOptions> streamCodec(Supplier<ParticleType<ExtraDataParticleOptions>> typeGetter) {
        return ByteBufCodecs.map(makeMap(), ByteBufCodecs.STRING_UTF8, ByteBufCodecs.FLOAT)
                .map(
                        map -> new ExtraDataParticleOptions(map, typeGetter.get()),
                        ExtraDataParticleOptions::extraData
                );
    }

    private static @NotNull IntFunction<Map<String, Float>> makeMap() {
        return i -> new HashMap<>();
    }

    @Override
    public ParticleType<?> getType() {
        return type;
    }

    public void apply(SingleQuadParticle particle) {
        if (extraData.isEmpty()) return;
        Float rot = extraData.get("roll");
        if (rot != null) {
            particle.roll = rot;
            particle.oRoll = rot;
        }
        Float red = extraData.get("red");
        if (red != null) {
            particle.rCol = red;
        }
        Float green = extraData.get("green");
        if (green != null) {
            particle.gCol = green;
        }
        Float blue = extraData.get("blue");
        if (blue != null) {
            particle.bCol = blue;
        }
        Float alpha = extraData.get("alpha");
        if (alpha != null) {
            particle.alpha = alpha;
        }
        Float size = extraData.get("size");
        if (size != null && particle instanceof SingleQuadParticle sp) {
            sp.quadSize = size;
        }
        Float custom = extraData.get("custom");
        if (custom != null && particle instanceof CustomParticleInstance inst) {
            inst.custom = custom;
        }
    }

}
