package net.mehvahdjukaar.polytone.tint;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class JsonBlockColorManager extends SimpleJsonResourceReloadListener {

    private static final BiMap<ResourceLocation, JsonBlockColor> COLORS = HashBiMap.create();


    private static final Codec<JsonBlockColor> REFERENCE = ResourceLocation.CODEC.flatXmap(
            id -> Optional.ofNullable(COLORS.get(id)).map(DataResult::success)
                    .orElse(DataResult.error(() -> "Unknown Color Property with id " + id)),
            object -> Optional.ofNullable(COLORS.inverse().get(object)).map(DataResult::success)
                    .orElse(DataResult.error(() -> "Unknown Color Property: " + object)));

    public static final Codec<JsonBlockColor> CODEC =
            ExtraCodecs.validate(Codec.either(REFERENCE, JsonBlockColor.CODEC)
                            .xmap(e -> e.map(Function.identity(), Function.identity()), Either::right),
                    j -> j.getters().size() == 0 ? DataResult.error(() -> "Must have at least 1 tint getter") :
                            DataResult.success(j));

    public JsonBlockColorManager() {
        super(new Gson(), "polytone/tint");
    }


    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        COLORS.clear();

        for (var j : map.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            CODEC.decode(JsonOps.INSTANCE, json)
                    // log error if parse fails
                    .resultOrPartial(errorMsg -> Polytone.LOGGER.warn("Could not decode Client Block Property with json id {} - error: {}", id, errorMsg))
                    // add loot modifier if parse succeeds
                    .ifPresent(modifier -> COLORS.put(id, modifier.getFirst()));


        }
        int aa = 1;
    }
}
