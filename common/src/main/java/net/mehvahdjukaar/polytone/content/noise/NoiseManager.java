package net.mehvahdjukaar.polytone.content.noise;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.mehvahdjukaar.polytone.common.exp.ExpressionUtils;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class NoiseManager extends ContentManager<PerlinSimplexNoise> {

    public static final SchemaCodec<PerlinSimplexNoise> NOISE_CODEC = SchemaRecord.create(PerlinSimplexNoise.class, i -> i.group(
            i.field("seed", Codec.INT, p -> 0),
            i.field("octaves", Codec.INT.listOf(), p -> List.of())
    ).apply(i, (s, l) -> new PerlinSimplexNoise(RandomSource.create(s), l)));

    public static final PerlinSimplexNoise DEFAULT =  new PerlinSimplexNoise(RandomSource.create(0), List.of(1));


    public NoiseManager() {
        super("Noise", () -> NOISE_CODEC, "noises");
    }

    private final MapRegistry<PerlinSimplexNoise> noises = new MapRegistry<>("Polytone Simplex Noises");

    @Nullable
    public PerlinSimplexNoise getNoise(String id) {
        return noises.getValue(id);
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        noises.clear();
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops,
                                  HolderLookup.Provider access) {
        Map<Identifier, JsonElement> jsons = resources.jsons();
        for (var e : jsons.entrySet()) {
            var id = e.getKey();
            var json = e.getValue();
            PerlinSimplexNoise noise = decodeStrict(json, id, ops);
            noises.register(id, noise);
        }
        ExpressionUtils.regenNoiseFunctions(noises.getEntries());
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
    }
}
