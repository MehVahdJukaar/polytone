package net.mehvahdjukaar.polytone.content.noise;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
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

public class NoiseManager extends JsonPartialReloader<NoiseManager.NoiseConfig> {

    // The editor needs a round-trippable codec, and PerlinSimplexNoise can't be re-encoded, so the
    // manager parses this config (seed + octaves) and builds the noise from it.
    public record NoiseConfig(int seed, List<Integer> octaves) {
        public static final Codec<NoiseConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("seed").forGetter(NoiseConfig::seed),
                Codec.INT.listOf().fieldOf("octaves").forGetter(NoiseConfig::octaves)
        ).apply(instance, NoiseConfig::new));

        public PerlinSimplexNoise build() {
            return new PerlinSimplexNoise(RandomSource.create(seed), octaves);
        }
    }

    public NoiseManager() {
        super(Spec.of("Noise", () -> NoiseConfig.CODEC).folders("noises").wikiPage("Math-Expressions"));
    }

    private final MapRegistry<PerlinSimplexNoise> noises = new MapRegistry<>("Polytone Simplex Noises");

    public PerlinSimplexNoise getNoise(String name) {
        return noises.getValue(name);
    }

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
            NoiseConfig config = NoiseConfig.CODEC.parse(ops, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Noise with json id " + id + "\n error: " + errorMsg));
            noises.register(id, config.build());
        }
        ExpressionUtils.regenNoiseFunctions(noises.getEntries());
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
    }
}
