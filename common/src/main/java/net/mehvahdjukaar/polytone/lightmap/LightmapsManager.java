package net.mehvahdjukaar.polytone.lightmap;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.mehvahdjukaar.polytone.utils.LegacyHelper;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.RegistryAccess;
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
        this.checkConditions(jsons);

        Map<ResourceLocation, ArrayImage> textures = new HashMap<>();

        Map<ResourceLocation, ArrayImage> ofTextures = ArrayImage.scanDirectory(resourceManager, "optifine/lightmap");
        Map<ResourceLocation, ArrayImage> cmTextures = ArrayImage.scanDirectory(resourceManager, "colormatic/lightmap");

        textures.putAll(LegacyHelper.convertPaths(ofTextures));
        textures.putAll(LegacyHelper.convertPaths(cmTextures));

        textures.putAll(this.getImagesInDirectories(resourceManager));

        return new Resources(ImmutableMap.copyOf(jsons), ImmutableMap.copyOf(textures));
    }

    @Override
    protected void parseWithLevel(Resources resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
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
            Lightmap lightmap;
            if (j != null) {
                lightmap = Lightmap.DIRECT_CODEC.decode(ops, j)
                        .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Lightmap with json id " + location + "\n error: " + errorMsg))
                        .getFirst();

            } else {
                //default samplers
                lightmap = new Lightmap();
            }

            var map = e.getValue();
            lightmap.acceptImages(map.get("normal"), map.get("rain"), map.get("thunder"));

            lightmaps.register(location, lightmap);
        }

        if (!jsons.isEmpty()) {
            throw new IllegalStateException("Found some lightmaps .jsons with no associated textures at" + jsons);
        }
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {

    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        lightmaps.clear();
    }

    private boolean reachedMainMenuHack = false;

    public boolean maybeModifyLightTexture(LightTexture instance,
                                           TextureTarget lightmap,
                                           Minecraft minecraft, ClientLevel level,
                                           float flicker, float partialTicks) {
        if (lastDimension != level.dimension()) {
            reachedMainMenuHack = true;
            lastDimension = level.dimension();
            currentLightmap = lightmaps.getValue(lastDimension.location());
        }
        if (usingGuiLightmap) {
            int aa = 1;//error
        }
        if (currentLightmap != null) {
            // if(true)return false;
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
