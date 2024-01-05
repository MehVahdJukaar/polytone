package net.mehvahdjukaar.polytone.tint;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class JsonBlockColorManager extends SimpleJsonResourceReloadListener {

    private static final BiMap<ResourceLocation, JsonBlockColor> COLORS = HashBiMap.create();


    private static final Codec<JsonBlockColor> REFERENCE = ResourceLocation.CODEC.flatXmap(
            id -> Optional.ofNullable(get(id)).map(DataResult::success)
                    .orElse(DataResult.error(() -> "Unknown Color Property with id " + id)),
            object -> Optional.ofNullable(getId(object)).map(DataResult::success)
                    .orElse(DataResult.error(() -> "Unknown Color Property: " + object)));

    public static final Codec<JsonBlockColor> CODEC = Codec.either(REFERENCE, JsonBlockColor.CODEC)
            .xmap(e -> e.map(Function.identity(), Function.identity()), Either::right);

    public JsonBlockColorManager() {
        super(new Gson(), "tint");
    }


    @Nullable
    public static JsonBlockColor get(ResourceLocation id) {
        return COLORS.get(id);
    }

    @Nullable
    public static ResourceLocation getId(JsonBlockColor object) {
        return COLORS.inverse().get(object);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        COLORS.clear();
    }
}
