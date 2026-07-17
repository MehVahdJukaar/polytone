package net.mehvahdjukaar.polytone.utils;

import com.google.common.base.Suppliers;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.polytone.companion.CompanionSpec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public abstract class ContentManager<O, T> extends PartialReloader<T> {

    public static final List<ContentManager<?, ?>> REGISTRY = new CopyOnWriteArrayList<>();

    /**
     * Editor bridge, set by PolytoneNautilus when the Nautilus Studio mod is present. Resolves
     * {@code (content folder, id)} to the raw json of that content file inside the pack currently
     * open in the editor, so by-name references to content the running game hasn't loaded still
     * decode there. Null (or a null result) everywhere else - reference decode then fails normally.
     */
    public static @Nullable BiFunction<String, ResourceLocation, @Nullable JsonElement> editorWorkspaceJsonLookup;

    private final Supplier<@Nullable ? extends Codec<O>> contentCodec;
    public final String name;
    public final @Nullable CompanionSpec<O> companions;
    private final @Nullable String wikiPage;

    protected ContentManager(Spec<O> spec) {
        super(spec.folderNames);
        this.name = spec.name;
        this.contentCodec = spec.codec == null ? () -> null : Suppliers.memoize(spec.codec::get);
        this.companions = spec.companions;
        this.wikiPage = spec.wikiPage;
        REGISTRY.add(this);
    }

    /** Fluent, order-independent replacement for a telescoping constructor. */
    public static final class Spec<O> {
        private final String name;
        private String[] folderNames = new String[0];
        private @Nullable Supplier<? extends Codec<O>> codec;
        private @Nullable CompanionSpec<O> companions;
        private @Nullable String wikiPage;

        private Spec(String name) {
            this.name = name;
        }

        /** For codec-less (non-editable) managers - O can't be inferred here, so give it explicitly: {@code Spec.<Foo>of(name)}. */
        public static <O> Spec<O> of(String name) {
            return new Spec<>(name);
        }

        /** O is inferred from the codec supplier, so callers never need a type witness. */
        public static <O> Spec<O> of(String name, Supplier<? extends Codec<O>> codec) {
            return new Spec<O>(name).codec(codec);
        }

        public Spec<O> codec(Supplier<? extends Codec<O>> codec) {
            this.codec = codec;
            return this;
        }

        public Spec<O> companions(CompanionSpec<O> companions) {
            this.companions = companions;
            return this;
        }

        public Spec<O> wikiPage(String wikiPage) {
            this.wikiPage = wikiPage;
            return this;
        }

        public Spec<O> folders(String... folders) {
            this.folderNames = folders;
            return this;
        }
    }

    public @Nullable Codec<O> contentCodec() {
        return contentCodec.get();
    }

    public Iterable<String> folderNames() {
        return List.of(names);
    }

    public @Nullable String primaryFolder() {
        return names.length == 0 ? null : names[0];
    }

    /** Wiki page slug shown as a help link in the pack editor. Managers without one just get no button. */
    public @Nullable String wikiPage() {
        return wikiPage;
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
