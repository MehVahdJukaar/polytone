package net.mehvahdjukaar.polytone.noise;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.ExpressionUtils;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;
import java.util.Map;

public class NoiseManager extends JsonPartialReloader {

    public static final Decoder<PerlinSimplexNoise> NOISE_CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            Codec.INT.fieldOf("seed").forGetter(p -> 0),
            Codec.INT.listOf().fieldOf("octaves").forGetter(p -> List.of())
    ).apply(instance, (s, l) -> new PerlinSimplexNoise(RandomSource.create(s), l)));


    public NoiseManager() {
        super("noises");
    }

    private final MapRegistry<PerlinSimplexNoise> noises = new MapRegistry<>("Polytone Simplex Noises");

    @Override
    protected void resetWithLevel(boolean logOff) {
        noises.clear();
    }

    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops,
                                  RegistryAccess access) {
        for (var e : jsons.entrySet()) {
            var id = e.getKey();
            var json = e.getValue();
            PerlinSimplexNoise noise = NOISE_CODEC.decode(ops, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Noise with json id " + id + "\n error: " + errorMsg))
                    .getFirst();
            noises.register(id, noise);
        }
        ExpressionUtils.regenNoiseFunctions(noises.getEntries());
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
    }
}
