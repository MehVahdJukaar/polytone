package net.mehvahdjukaar.polytone.utils;

import com.google.common.base.Suppliers;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.polytone.companion.ContentTextures;
import net.mehvahdjukaar.polytone.companion.TexturePart;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public abstract class ContentManager<O, T> extends PartialReloader<T> {

    public static final List<ContentManager<?, ?>> REGISTRY = new CopyOnWriteArrayList<>();

    private final Supplier<@Nullable ? extends Codec<O>> contentCodec;
    public final String name;
    public final @Nullable ContentTextures<O> contentTexture;
    private final @Nullable String wikiPage;

    protected ContentManager(Spec<O> spec) {
        super(spec.folderNames);
        this.name = spec.name;
        this.contentCodec = spec.codec == null ? () -> null : Suppliers.memoize(spec.codec::get);
        this.contentTexture = spec.buildCompanionTextures();
        this.wikiPage = spec.wikiPage;
        REGISTRY.add(this);
    }

    public static final class Spec<O> {
        private final String name;
        private String[] folderNames = new String[0];
        private @Nullable Supplier<? extends Codec<O>> codec;
        private @Nullable List<TexturePart<O>> textureParts;
        private @Nullable String wikiPage;

        private Spec(String name) {
            this.name = name;
        }

        // O can't be inferred without a codec, so give it explicitly: Spec.<Foo>of(name)
        public static <O> Spec<O> of(String name) {
            return new Spec<>(name);
        }

        public static <O> Spec<O> of(String name, Supplier<? extends Codec<O>> codec) {
            return new Spec<O>(name).codec(codec);
        }

        public Spec<O> codec(Supplier<? extends Codec<O>> codec) {
            this.codec = codec;
            return this;
        }

        // order matters: the first part is the main feature, claiming plain <stem>.png files nothing else
        // explains
        @SafeVarargs
        public final Spec<O> textureParts(TexturePart<O>... parts) {
            this.textureParts = List.of(parts);
            return this;
        }

        private @Nullable ContentTextures<O> buildCompanionTextures() {
            return textureParts == null ? null : new ContentTextures<>(textureParts);
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

    public @Nullable String wikiPage() {
        return wikiPage;
    }

    protected final Iterable<Map.Entry<ResourceLocation, O>> parseEnabledJsons(
            Map<ResourceLocation, JsonElement> jsons, DynamicOps<JsonElement> ops) {
        return Parsed.batchParseOnlyEnabled(jsons, this.contentCodec(), ops, name);
    }

    protected final Parsed.SortedMap<O> parseAllJsons(
            Map<ResourceLocation, JsonElement> jsons, DynamicOps<JsonElement> ops) {
        return Parsed.batchParseAlways(jsons, this.contentCodec(), ops, name);
    }

    protected final Parsed.SortedMap<O> parseJsonsOrPartial(
            Map<ResourceLocation, JsonElement> jsons, Decoder<O> partialCodec, DynamicOps<JsonElement> ops) {
        return Parsed.batchParseOrPartial(jsons, this.contentCodec(), partialCodec, ops, name);
    }

    protected final Parsed<O> parseJson(JsonElement json, ResourceLocation id, DynamicOps<JsonElement> ops) {
        return Parsed.parseAlways(this.contentCodec(), json, ops, id, name);
    }

    protected final O decodeStrict(JsonElement json, ResourceLocation id, DynamicOps<JsonElement> ops) {
        return this.contentCodec().decode(ops, json)
                .getOrThrow(msg -> new IllegalStateException(
                        "Could not decode " + name + " with json id " + id + "\n error: " + msg))
                .getFirst();
    }

    @Override
    public String toString() {
        return StrUtils.readableName(name) + " Reloader";
    }
}
