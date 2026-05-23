package net.mehvahdjukaar.polytone.content.lightmap;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.LegacyHelper;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.Targets;
import net.mehvahdjukaar.polytone.common.reloader.JsonImgPartialReloader;
import net.mehvahdjukaar.polytone.common.struc.ArrayImage;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class LightmapsManager extends JsonImgPartialReloader {

    public static final Identifier GUI_LIGHTMAP = Polytone.res("lightmaps/gui.png");
    private static final Identifier DEFAULT_LIGHTMAP = Identifier.withDefaultNamespace("default");

    private static final Codec<Targets> TARGET_ONLY_CODEC = Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY)
            .codec();

    //lightmap id to lightmap
    private final MapRegistry<Lightmap> lightmaps = new MapRegistry<>("Lightmaps");
    //TODO:
    private final Map<ResourceKey<Biome>, Lightmap> biomeLightmaps = new HashMap<>();

    private ResourceKey<Level> lastDimension = null;
    private Lightmap currentLightmap = null;

    public LightmapsManager() {
        super("lightmaps");
    }

    @Override
    protected Resources prepare(PreparableReloadListener.SharedState sharedState) {
        ResourceManager resourceManager = sharedState.resourceManager();
        var jsons = this.getJsonsInDirectories(resourceManager);

        Map<Identifier, ArrayImage> textures = new HashMap<>();

        Map<Identifier, ArrayImage> ofTextures = ArrayImage.scanDirectory(resourceManager, "optifine/lightmap");
        Map<Identifier, ArrayImage> cmTextures = ArrayImage.scanDirectory(resourceManager, "colormatic/lightmap");

        textures.putAll(LegacyHelper.convertPaths(ofTextures));
        textures.putAll(LegacyHelper.convertPaths(cmTextures));

        textures.putAll(this.getImagesInDirectories(resourceManager));

        return new Resources(ImmutableMap.copyOf(jsons), ImmutableMap.copyOf(textures));
    }

    @Override
    protected void parseWithLevel(Resources resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        var images = resources.textures();
        var jsons = new HashMap<>(resources.jsons());
        lastDimension = null;
        currentLightmap = null;

        Map<Identifier, Map<String, ArrayImage>> grouped = new HashMap<>();
        for (var e : images.entrySet()) {
            ArrayImage value = e.getValue();
            int height = value.height();

            Identifier location = e.getKey();
            if (height != 16 && height != 32 && height != 64) {
                throw new IllegalStateException("Lightmap must be either 16, 32 or 64 pixels tall. Provided one at " + location + " was " + height + " pixels");
            } else {
                String path = location.getPath();
                if (path.endsWith("_thunder")) {
                    grouped.computeIfAbsent(location.withPath(path.replace("_thunder", "")),
                            g -> new HashMap<>()).put("thunder", value);
                } else if (path.endsWith("_rain")) {
                    grouped.computeIfAbsent(location.withPath(path.replace("_rain", "")),
                            g -> new HashMap<>()).put("rain", value);
                } else {
                    grouped.computeIfAbsent(location, g -> new HashMap<>()).put("normal", value);
                }
            }
        }

        for (var e : grouped.entrySet()) {
            Identifier location = e.getKey();

            JsonElement j = jsons.remove(location);
            Parsed<Lightmap> parsed;
            Lightmap lightmap;
            if (j != null) {
                parsed = Parsed.parseAlways(Lightmap.CODEC, j, ops, location, "lightmap");
            } else {
                //default samplers
                parsed = Parsed.success(new Lightmap(), location);
            }
            lightmap = parsed.getResultOrPartial();

            var map = e.getValue();
            lightmap.acceptImages(map.get("normal"), map.get("rain"), map.get("thunder"));

            if (parsed.isEnabled()) {
                addLightmap(location, lightmap, access);
            }
        }

        if (!jsons.isEmpty()) {
            throw new IllegalStateException("Found some lightmaps .jsons with no associated textures at" + jsons);
        }
    }

    private void addLightmap(Identifier fileId, Lightmap mod, HolderLookup.Provider access) {
        for (var dim : mod.targets().compute(fileId, access)) {
            lightmaps.register(dim.unwrapKey().get().identifier(), mod);
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {

    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        lightmaps.clear();
    }

    private boolean reachedMainMenuHack = false;

    public boolean maybeModifyLightTexture(Object instance,
                                           GpuTextureView lightmap,
                                           Minecraft minecraft, ClientLevel level,
                                           float flicker, float partialTicks) {
        if (true) return false; //TODO: add back as shader
        if (lastDimension != level.dimension()) {
            reachedMainMenuHack = true;
            lastDimension = level.dimension();
            currentLightmap = findLightmapForLevel(level);
            if (currentLightmap == null) {
                currentLightmap = lightmaps.getValue(DEFAULT_LIGHTMAP);
            }
            if (currentLightmap != null) {
                currentLightmap.forceRefresh();
            }
        }
        if (currentLightmap != null) {
            currentLightmap.applyToLightTexture(instance, lightmap, minecraft,
                    level, flicker, partialTicks);
            return true;
        }
        return false;
    }


    private boolean usingGuiLightmap = false;

    public void setupForGUI(boolean gui) {
        usingGuiLightmap = gui;
    }

    public boolean isGui() {
        if (!reachedMainMenuHack && !PlatStuff.isModStateValid()) {
            return false;
        }
        return usingGuiLightmap;
    }

    public Codec<Lightmap> byNameCodec() {
        return lightmaps;
    }

    private @Nullable Lightmap findLightmapForLevel(Level level) {
        var currentDimHolder = level.dimensionTypeRegistration();
        RegistryAccess access = level.registryAccess();
        for (var v : lightmaps.getEntries()) {
            Identifier modId = v.getKey();
            Lightmap modifier = v.getValue();
            var targets = modifier.targets().compute(modId, access);
            if (targets.contains(currentDimHolder)) {
                return modifier;
            }
        }
        return null;
    }

    public AbstractTexture getGuiLightTexture() {
        return Minecraft.getInstance().getTextureManager().getTexture(GUI_LIGHTMAP);
    }
}
