package net.mehvahdjukaar.polytone.lightmap;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;

public class LightmapsManager extends JsonImgPartialReloader {

    public static final ResourceLocation GUI_LIGHTMAP = Polytone.res("lightmaps/gui.png");
    private static final ResourceLocation DEFAULT_LIGHTMAP = ResourceLocation.withDefaultNamespace("default");

    private static final Codec<Targets> TARGET_ONLY_CODEC = Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY)
            .codec();

    private final MapRegistry<Lightmap> lightmaps = new MapRegistry<>("Lightmaps");
    //TODO:
    private final Map<ResourceKey<Biome>, Lightmap> biomeLightmaps = new HashMap<>();

    private ResourceKey<Level> lastDimension = null;
    private Lightmap currentLightmap = null;

    public LightmapsManager() {
        super("lightmaps");
    }

    @Override
    protected Resources prepare(ResourceManager resourceManager) {
        var jsons = this.getJsonsInDirectories(resourceManager);

        Map<ResourceLocation, ArrayImage> textures = new HashMap<>();

        Map<ResourceLocation, ArrayImage> ofTextures = ArrayImage.scanDirectory(resourceManager, "optifine/lightmap");
        Map<ResourceLocation, ArrayImage> cmTextures = ArrayImage.scanDirectory(resourceManager, "colormatic/lightmap");

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

        Map<ResourceLocation, Map<String, ArrayImage>> grouped = new HashMap<>();
        for (var e : images.entrySet()) {
            ArrayImage value = e.getValue();
            int height = value.height();

            ResourceLocation location = e.getKey();
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
            ResourceLocation location = e.getKey();

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

    private void addLightmap(ResourceLocation fileId, Lightmap mod, HolderLookup.Provider access) {
        for (var dim : mod.targets().getTargets(fileId, access)) {
            lightmaps.register(dim.unwrapKey().get().location(), mod);
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

    public boolean maybeModifyLightTexture(LightTexture instance,
                                           GpuTextureView lightmap,
                                           Minecraft minecraft, ClientLevel level,
                                           float flicker, float partialTicks) {
        if (lastDimension != level.dimension()) {
            reachedMainMenuHack = true;
            lastDimension = level.dimension();
            currentLightmap = lightmaps.getValue(lastDimension.location());
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
}
