package net.mehvahdjukaar.polytone.utils;

import com.google.common.base.Suppliers;
import com.google.gson.JsonElement;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.polytone.companion.CompanionSpec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public abstract class ContentManager<O, T> extends PartialReloader<T> {

    public static final List<ContentManager<?, ?>> REGISTRY = new CopyOnWriteArrayList<>();

    private final Supplier<@Nullable ? extends SchemaCodec<O>> contentCodec;
    public final String name;
    public final @Nullable CompanionSpec<O> companions;

    protected ContentManager(String name, String... folderNames) {
        this(name, null, null, folderNames);
    }

    protected ContentManager(String name, Supplier<? extends SchemaCodec<O>> codec, String... folderNames) {
        this(name, codec, null, folderNames);
    }

    protected ContentManager(String name, @Nullable Supplier<? extends SchemaCodec<O>> codec,
                             @Nullable CompanionSpec<O> companions, String... folderNames) {
        super(folderNames);
        this.name = name;
        this.contentCodec = codec == null ? () -> null : Suppliers.memoize(codec::get);
        this.companions = companions;
        REGISTRY.add(this);
    }

    public @Nullable SchemaCodec<O> contentCodec() {
        return contentCodec.get();
    }

    public Iterable<String> folderNames() {
        return List.of(names);
    }

    public @Nullable String primaryFolder() {
        return names.length == 0 ? null : names[0];
    }

    // -------------------- parse helpers (condition-aware, via the existing Parsed) --------------------

    /** Batch-decode jsons, yielding only entries whose conditions are met. */
    protected final Iterable<Map.Entry<ResourceLocation, O>> parseEnabledJsons(
            Map<ResourceLocation, JsonElement> jsons, DynamicOps<JsonElement> ops) {
        return Parsed.batchParseOnlyEnabled(jsons, this.contentCodec(), ops, name);
    }

    /** Batch-decode jsons, keeping condition-disabled entries too (as {@link Parsed}). */
    protected final Parsed.SortedMap<O> parseAllJsons(
            Map<ResourceLocation, JsonElement> jsons, DynamicOps<JsonElement> ops) {
        return Parsed.batchParseAlways(jsons, this.contentCodec(), ops, name);
    }

    /** Batch-decode jsons; condition-disabled entries decode with {@code partialCodec} instead. */
    protected final Parsed.SortedMap<O> parseJsonsOrPartial(
            Map<ResourceLocation, JsonElement> jsons, Decoder<O> partialCodec, DynamicOps<JsonElement> ops) {
        return Parsed.batchParseOrPartial(jsons, this.contentCodec(), partialCodec, ops, name);
    }

    /** Decode one file (condition- and lenient-loading-aware). */
    protected final Parsed<O> parseJson(JsonElement json, ResourceLocation id, DynamicOps<JsonElement> ops) {
        return Parsed.parseAlways(this.contentCodec(), json, ops, id, name);
    }

    /** Decode one file, throwing on any error (no condition handling). */
    protected final O decodeStrict(JsonElement json, ResourceLocation id, DynamicOps<JsonElement> ops) {
        return this.contentCodec().decode(ops, json)
                .getOrThrow(msg -> new IllegalStateException(
                        "Could not decode " + name + " with json id " + id + "\n error: " + msg))
                .getFirst();
    }

    @Override
    public String toString() {
        return org.apache.commons.lang3.StringUtils.capitalize(name.replace("_", " ")) + " Reloader";
    }
}
