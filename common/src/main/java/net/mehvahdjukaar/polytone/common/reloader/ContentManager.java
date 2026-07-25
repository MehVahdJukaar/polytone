package net.mehvahdjukaar.polytone.common.reloader;

import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.StrUtils;
import net.mehvahdjukaar.polytone.common.companion.ContentTextures;
import net.mehvahdjukaar.polytone.common.companion.TexturePart;
import net.mehvahdjukaar.polytone.common.struc.ArrayImage;
import net.mehvahdjukaar.polytone.common.struc.ListUtils;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class ContentManager<O> {

    public static final Gson GSON = new Gson();

    private final String[] folderNames;
    private final Supplier<@Nullable ? extends SchemaCodec<O>> contentCodec;
    public final String name;
    public final @Nullable ContentTextures<O> contentTexture;
    private final @Nullable String wikiPage;

    protected ContentManager(String name, String... folderNames) {
        this(Spec.<O>of(name).folders(folderNames));
    }

    protected ContentManager(String name, Supplier<? extends SchemaCodec<O>> codec, String... folderNames) {
        this(Spec.of(name, codec).folders(folderNames));
    }

    protected ContentManager(Spec<O> spec) {
        this.name = spec.name;
        this.contentCodec = spec.codec == null ? () -> null : Suppliers.memoize(spec.codec::get);
        this.contentTexture = spec.buildCompanionTextures();
        this.wikiPage = spec.wikiPage;
        this.folderNames = spec.folderNames;
    }

    public static final class Spec<O> {
        private final String name;
        private String[] folderNames = new String[0];
        private @Nullable Supplier<? extends SchemaCodec<O>> codec;
        private @Nullable List<TexturePart<O>> textureParts;
        private @Nullable String wikiPage;

        private Spec(String name) {
            this.name = name;
        }

        public static <O> Spec<O> of(String name) {
            return new Spec<>(name);
        }

        public static <O> Spec<O> of(String name, Supplier<? extends SchemaCodec<O>> codec) {
            return new Spec<O>(name).codec(codec);
        }

        public Spec<O> codec(Supplier<? extends SchemaCodec<O>> codec) {
            this.codec = codec;
            return this;
        }

        /**
         * Declares the companion textures this content type carries. Order matters: the first
         * part is the main feature, claiming plain {@code <stem>.png} files nothing else explains.
         * The manager's parse pass drives adoption and orphan creation itself, off
         * {@code contentTexture.adoptable}/{@code orphans}.
         */
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

    public @Nullable SchemaCodec<O> contentCodec() {
        return contentCodec.get();
    }

    public Iterable<String> folderNames() {
        return Arrays.stream(folderNames).toList();
    }

    public @Nullable String primaryFolder() {
        return folderNames.length == 0 ? null : folderNames[0];
    }

    public @Nullable String wikiPage() {
        return wikiPage;
    }
    /**
     * Batch-decode jsons, yielding only entries whose conditions are met.
     */
    protected final Iterable<Map.Entry<Identifier, O>> parseEnabledJsons(Map<Identifier, JsonElement> jsons, DynamicOps<JsonElement> ops) {
        return Parsed.batchParseOnlyEnabled(jsons, this.contentCodec(), ops, name);
    }

    /**
     * Batch-decode jsons, keeping condition-disabled entries too (as {@link Parsed}).
     */
    protected final Parsed.SortedMap<O> parseAllJsons(Map<Identifier, JsonElement> jsons, DynamicOps<JsonElement> ops) {
        return Parsed.batchParseAlways(jsons, this.contentCodec(), ops, name);
    }

    /**
     * Batch-decode jsons; condition-disabled entries decode with {@code partialCodec} instead.
     */
    protected final Parsed.SortedMap<O> parseJsonsOrPartial(Map<Identifier, JsonElement> jsons,
                                                                Decoder<O> partialCodec,
                                                                DynamicOps<JsonElement> ops) {
        return Parsed.batchParseOrPartial(jsons, this.contentCodec(), partialCodec, ops, name);
    }

    /**
     * Decode one file (condition- and lenient-loading-aware).
     */
    protected final Parsed<O> parseJson(JsonElement json, Identifier id, DynamicOps<JsonElement> ops) {
        return Parsed.parseAlways(this.contentCodec(), json, ops, id, name);
    }

    /**
     * Decode one file, throwing on any error (no condition handling).
     */
    protected final O decodeStrict(JsonElement json, Identifier id, DynamicOps<JsonElement> ops) {
        return this.contentCodec().decode(ops, json)
                .getOrThrow(msg -> new IllegalStateException(
                        "Could not decode " + name + " with json id " + id + "\n error: " + msg))
                .getFirst();
    }

    @Override
    public String toString() {
        return StrUtils.readableName(name) + " Reloader";
    }

    protected Map<Identifier, JsonElement> getJsonsInDirectories(ResourceManager resourceManager) {
        // resources given by the resource manager won't be sorted by pack ordering so we at least sort them by name
        Map<Identifier, JsonElement> jsons = ListUtils.sortedMap();
        for (String name : folderNames) {
            Map<Identifier, JsonElement> js = new HashMap<>();
            scanDirectory(resourceManager, Polytone.MOD_ID + "/" + name, GSON, js);
            greedyAddAll(js, jsons);
        }
        //sort by key
        return jsons;
    }

    public static void scanDirectory(ResourceManager resourceManager, String string, Gson gson, Map<Identifier, JsonElement> map) {
        FileToIdConverter fileToIdConverter = FileToIdConverter.json(string);

        for (Map.Entry<Identifier, Resource> entry : fileToIdConverter.listMatchingResources(resourceManager).entrySet()) {
            Identifier resourceLocation = entry.getKey();
            Identifier resourceLocation2 = fileToIdConverter.fileToId(resourceLocation);

            try (Reader reader = entry.getValue().openAsReader()) {

                JsonElement jsonElement = GsonHelper.fromJson(gson, reader, JsonElement.class);
                JsonElement jsonElement2 = map.put(resourceLocation2, jsonElement);
                if (jsonElement2 != null) {
                    Polytone.maybeThrow(
                            new IllegalStateException("Duplicate data file ignored with ID " + resourceLocation2)
                    );
                }

            } catch (IllegalArgumentException | IOException | JsonParseException var14) {
                Polytone.maybeThrow(
                        new IllegalStateException("Couldn't parse data file " + resourceLocation2 + " from " + resourceLocation, var14)
                );
                // Polytone.LOGGER.error("Couldn't parse data file {} from {}", resourceLocation2, resourceLocation, var14);
            }
        }
    }

    private static <T> void greedyAddAll(Map<Identifier, T> js, Map<Identifier, T> jsons) {
        for (var entry : js.entrySet()) {
            var r = entry.getKey();
            var j = entry.getValue();
            jsons.put(r, j);
        }
    }

    protected Map<Identifier, ArrayImage> getImagesInDirectories(ResourceManager resourceManager) {
        Map<Identifier, ArrayImage> images = new HashMap<>();
        for (String name : folderNames) {
            Map<Identifier, ArrayImage> im = new HashMap<>();
            ArrayImage.scanDirectory(resourceManager, Polytone.MOD_ID + "/" + name, im);
            greedyAddAll(im, images);
        }
        return images;
    }

    protected void earlyProcess(PreparableReloadListener.SharedState sharedState) {
    }

    /**
     * Scan this manager's folder(s) off-thread and group the files by type. The default gathers
     * every {@code .json} and sibling {@code .png}; managers that need more (extra scan paths,
     * csv sidecars) override, stash the extras in their own fields, and return this bundle.
     */
    protected AssetsFiles prepare(PreparableReloadListener.SharedState sharedState) {
        var resourceManager = sharedState.resourceManager();
        return new AssetsFiles(getJsonsInDirectories(resourceManager),
                getImagesInDirectories(resourceManager));
    }

    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
    }


    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
    }


    protected void resetWithLevel(boolean logOff) {
    }

    protected void applyNormal(AssetsFiles resources) {
    }

}
