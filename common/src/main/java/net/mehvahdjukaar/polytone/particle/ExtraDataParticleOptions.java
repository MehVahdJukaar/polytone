package net.mehvahdjukaar.polytone.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public record ExtraDataParticleOptions(Map<String, Float> extraData,
                                       ParticleType<?> type) implements ParticleOptions {

    public static Codec<ExtraDataParticleOptions> codec(Supplier<ParticleType<ExtraDataParticleOptions>> typeGetter) {
        return Codec.unboundedMap(Codec.STRING, Codec.FLOAT)
                .optionalFieldOf("extra_data", Map.of())
                .xmap(stringFloatMap -> new ExtraDataParticleOptions(stringFloatMap, typeGetter.get()), ExtraDataParticleOptions::extraData)
                .codec();

    }

    private static @NotNull IntFunction<Map<String, Float>> makeMap() {
        return i -> new HashMap<>();
    }

    @Override
    public ParticleType<?> getType() {
        return type;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeVarInt(extraData.size());
        for (Map.Entry<String, Float> entry : extraData.entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeFloat(entry.getValue());
        }
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s", BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()));
    }

    public void apply(Particle particle) {
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
        if (custom != null && particle instanceof CustomParticleType.Instance inst) {
            inst.custom = custom;
        }
    }


    public static final ParticleOptions.Deserializer<ExtraDataParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public ExtraDataParticleOptions fromCommand(ParticleType<ExtraDataParticleOptions> arg, StringReader stringReader) {
            return new ExtraDataParticleOptions(new HashMap<>(), arg);
        }

        @Override
        public ExtraDataParticleOptions fromNetwork(ParticleType<ExtraDataParticleOptions> arg, FriendlyByteBuf arg2) {
            int size = arg2.readVarInt();
            Map<String, Float> data = makeMap().apply(size);
            for (int i = 0; i < size; i++) {
                String key = arg2.readUtf();
                float value = arg2.readFloat();
                data.put(key, value);
            }
            return new ExtraDataParticleOptions(data, arg);
        }
    };

}
