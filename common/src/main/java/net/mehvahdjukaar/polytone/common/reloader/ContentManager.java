package net.mehvahdjukaar.polytone.common.reloader;

import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.StrUtils;
import net.mehvahdjukaar.polytone.common.Utils;
import net.mehvahdjukaar.polytone.common.companion.ContentTextures;
import net.mehvahdjukaar.polytone.common.companion.TexturePart;
import net.mehvahdjukaar.polytone.common.struc.ArrayImage;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import static net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener.scanDirectory;

public abstract class ContentManager<O> {

    public static final List<ContentManager<?>> REGISTRY = new CopyOnWriteArrayList<>();
    public static final Gson GSON = new Gson();

    private final String[] folderNames;

    private final Supplier<@Nullable ? extends Codec<O>> contentCodec;
    public final String name;
    public final @Nullable ContentTextures<O> contentTexture;
    private final @Nullable String wikiPage;

    protected ContentManager(String name, String... folderNames) {
        this(Spec.<O>of(name).folders(folderNames));
    }

    protected ContentManager(String name, Supplier<? extends Codec<O>> codec, String... folderNames) {
        this(Spec.of(name, codec).folders(folderNames));
    }

    protected ContentManager(Spec<O> spec) {
        this.folderNames = spec.folderNames;
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

    public List<String> folderNames() {
        return List.of(folderNames);
    }

    public @Nullable String primaryFolder() {
        return folderNames.length == 0 ? null : folderNames[0];
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

    protected Map<ResourceLocation, JsonElement> getJsonsInDirectories(ResourceManager resourceManager) {
        // resources given by the resource manager won't be sorted by pack ordering so we at least sort them by name
        Map<ResourceLocation, JsonElement> jsons = Utils.sortedMap();
        for (String folder : folderNames) {
            Map<ResourceLocation, JsonElement> js = new HashMap<>();
            scanDirectory(resourceManager, Polytone.MOD_ID + "/" + folder, GSON, js);
            jsons.putAll(js);
        }
        return jsons;
    }

    protected Map<ResourceLocation, ArrayImage> getImagesInDirectories(ResourceManager resourceManager) {
        Map<ResourceLocation, ArrayImage> images = new HashMap<>();
        for (String folder : folderNames) {
            images.putAll(ArrayImage.scanDirectory(resourceManager, Polytone.MOD_ID + "/" + folder));
        }
        return images;
    }

    protected Map<ResourceLocation, ArrayImage.Group> getGroupedImagesInDirectories(ResourceManager manager) {
        return ArrayImage.groupTextures(this.getImagesInDirectories(manager));
    }

    protected void earlyProcess(ResourceManager resourceManager) {
    }

    // Scan this manager's folder(s) off-thread and group the files by type. The default gathers every .json
    // and sibling .png; managers that need more (extra scan paths, csv sidecars) override, stash the extras in
    // their own fields, and return this bundle.
    protected AssetsFiles prepare(ResourceManager resourceManager) {
        return new AssetsFiles(getJsonsInDirectories(resourceManager), getImagesInDirectories(resourceManager));
    }

    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
    }

    // Reloads with no world loaded have no RegistryAccess to decode against, so only managers whose
    // content decodes with plain JsonOps can do anything here; everyone else stays deferred until login.
    protected void parseWithoutLevel(AssetsFiles resources) {
    }

    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
    }

    protected void resetWithLevel(boolean logOff) {
    }
}
