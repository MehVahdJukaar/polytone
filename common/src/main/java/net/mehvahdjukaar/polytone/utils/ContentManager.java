package net.mehvahdjukaar.polytone.utils;

import com.google.common.base.Suppliers;
import com.google.gson.JsonElement;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.polytone.companion.CompanionSpec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A {@link PartialReloader} that also carries the two things the pack editor needs: a
 * {@link SchemaCodec} describing the content type ({@link #contentCodec()}) and an optional
 * {@link CompanionSpec} describing its sidecar files ({@link #companions}). Managers that expose
 * these become editable in Nautilus Studio; the reload lifecycle is unchanged from
 * {@code PartialReloader}, so converting a manager is opt-in and incremental.
 *
 * <p>1.21.1 port of the 1.21.11 {@code common.reloader.ContentManager}: that version replaced the
 * reload base outright and drove off the newer {@code PreparableReloadListener.SharedState}
 * lifecycle. Here we keep 1.21.1's {@link PartialReloader} lifecycle and layer the editor-facing
 * API on top, so existing {@code PartialReloader} managers keep working untouched.</p>
 *
 * @param <O> the decoded content type this manager's files parse into
 */
public abstract class ContentManager<O> extends PartialReloader<AssetsFiles> {

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
    }

    /** The schema-aware file codec for this content type, or null when this manager isn't editable. */
    public @Nullable SchemaCodec<O> contentCodec() {
        return contentCodec.get();
    }

    public Iterable<String> folderNames() {
        return List.of(names);
    }

    // -------------------- parse helpers (condition-aware, via Parsed) --------------------

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

    // -------------------- reload lifecycle (defaults; subclasses override selectively) --------------------

    /** Default: gather every {@code .json} and sibling {@code .png} in this manager's folders. */
    @Override
    protected AssetsFiles prepare(ResourceManager resourceManager) {
        return new AssetsFiles(getJsonsInDirectories(resourceManager), getImagesInDirectories(resourceManager));
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
    }
}
