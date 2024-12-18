package net.mehvahdjukaar.polytone.colormap;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class ColormapsManager extends JsonImgPartialReloader {

    // Builtin colormaps
    //TODO: delegate to grass so we have quark compat
    public static final IColorGetter GRASS_COLOR = new IColorGetter.OfBlock((s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageGrassColor(l, p) : GrassColor.getDefaultColor());

    public static final IColorGetter FOLIAGE_COLOR = new IColorGetter.OfBlock((s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageFoliageColor(l, p) : FoliageColor.FOLIAGE_DEFAULT);

    public static final IColorGetter WATER_COLOR = new IColorGetter.OfBlock((s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageWaterColor(l, p) : 0xFF000000);

    // custom defined colormaps
    private final MapRegistry<Supplier<IColorGetter>> colormaps = new MapRegistry<>("Polytone Colormaps");

    public Codec<IColorGetter> byNameCodec() {
        return colormaps.xmap(Supplier::get, s -> () -> s);
    }

    public ColormapsManager() {
        super("colormaps");
    }

    @Override
    protected void parseWithLevel(Resources resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        var jsons = resources.jsons();
        var textures = new HashMap<>(resources.textures());

        Set<ResourceLocation> usedTextures = new HashSet<>();

        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            Colormap colormap = Colormap.DIRECT_CODEC.decode(ops, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Colormap with json id " + id + "\n error: " + errorMsg))
                    .getFirst();
            colormap.inlined = false;
            tryAcceptingTexture(textures, id, colormap, usedTextures, true);


            // we need to fill these before we parse the properties as they will be referenced below
            add(id, colormap);
        }


        // creates orphaned texture colormaps
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            ResourceLocation id = t.getKey();
            Colormap defaultColormap = Colormap.createDefTriangle();
            defaultColormap.inlined = false;
            tryAcceptingTexture(textures, id, defaultColormap, usedTextures, true);
            // we need to fill these before we parse the properties as they will be referenced below
            add(id, defaultColormap);
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {

    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        colormaps.clear();
        colormaps.register(ResourceLocation.parse("grass_color"), () -> GRASS_COLOR);
        colormaps.register(ResourceLocation.parse("foliage_color"), () -> FOLIAGE_COLOR);
        colormaps.register(ResourceLocation.parse("water_color"), () -> WATER_COLOR);
        //These create new incomplete ones every time
        colormaps.register(ResourceLocation.parse("biome_sample"), Colormap::createDefSquare);
        colormaps.register(ResourceLocation.parse("triangular_biome_sample"), Colormap::createDefTriangle);
        colormaps.register(ResourceLocation.parse("fixed"), Colormap::createFixed);
        colormaps.register(ResourceLocation.parse("grid"), Colormap::createBiomeId);
        colormaps.register(ResourceLocation.parse("damage"), Colormap::createDamage);
    }

    public void add(ResourceLocation id, Colormap colormap) {
        colormaps.register(id, () -> colormap);
        if (!colormap.hasTexture()) {
            throw new IllegalStateException("Did not find any texture png for colormap " + id);
        }
    }


    //helper methods
    public static void tryAcceptingTextureGroup(Map<ResourceLocation, ArrayImage.Group> availableTextures,
                                                ResourceLocation defaultPath, BlockColor col, Set<ResourceLocation> usedTexture, boolean strict) {
        if (col instanceof IndexCompoundColorGetter c) {
            tryAcceptingTextureGroup(availableTextures, defaultPath, c, usedTexture, strict);
        } else if (col instanceof Colormap c) {
            tryAcceptingTextureGroup(availableTextures, defaultPath, c, usedTexture, strict);
        }
    }

    private static void tryAcceptingTextureGroup(Map<ResourceLocation, ArrayImage.Group> availableTextures,
                                                 ResourceLocation defaultPath, Colormap c, Set<ResourceLocation> usedTexture, boolean strict) {
        ResourceLocation textureLoc = c.getTargetTexture(defaultPath);
        ArrayImage.Group group = availableTextures.get(textureLoc);
        ArrayImage texture = group != null ? group.getDefault() : null;
        tryAcceptingTexture(texture, textureLoc, c, usedTexture, strict);
    }

    private static void tryAcceptingTextureGroup(Map<ResourceLocation, ArrayImage.Group> textures,
                                                 ResourceLocation id, IndexCompoundColorGetter colormap,
                                                 Set<ResourceLocation> usedTextures, boolean strict) {
        var blockColorGetters = colormap.getGetters();

        for (var g : blockColorGetters.int2ObjectEntrySet()) {
            int index = g.getIntKey();
            BlockColor inner = g.getValue();

            if (inner instanceof Colormap c && !c.hasTexture()) {

                var textureMap = textures.get(c.getTargetTexture(id));

                if (strict && textureMap == null) {
                    throw new IllegalStateException("Could not find a texture for tint index " + index + " for compound colormap " + id + "." +
                            "Expected " + id + "_" + index);
                }

                if (blockColorGetters.size() == 1 || index == 0) {
                    //try twice. first time doesnt throw
                    tryAcceptingTexture(textureMap.getDefault(), id, c, usedTextures, false);
                }
                try {
                    tryAcceptingTexture(textureMap.get(index), id, c, usedTextures, strict);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to apply a texture for tint index " + index + " for compound colormap " + id + "." +
                            "Expected " + id + "_" + index + " : ", e);
                }
            }
        }

    }

    public static void tryAcceptingTexture(Map<ResourceLocation, ArrayImage> availableTextures,
                                           ResourceLocation defaultPath,
                                           @Nullable Object col, Set<ResourceLocation> usedTexture, boolean strict) {
        if (col instanceof Colormap colormap) {
            ResourceLocation textureLoc = colormap.getTargetTexture(defaultPath);
            ArrayImage texture = availableTextures.get(textureLoc);
            tryAcceptingTexture(texture, textureLoc, colormap, usedTexture, strict);
        }
    }

    private static void tryAcceptingTexture(@Nullable ArrayImage selectedTexture, ResourceLocation textureLoc, Colormap colormap,
                                            Set<ResourceLocation> usedTexture, boolean strict) {
        if (colormap.hasTexture()) return; //we already are filled
        //hack. for inlined this will be the parent modifier id.
        String colormapName = colormap.inlined ? "Inlined Colormap from modifier " + textureLoc.toString() : "Colormap at " + textureLoc.toString();

        if (selectedTexture != null) {
            usedTexture.add(textureLoc);
            colormap.acceptTexture(selectedTexture);
            if (selectedTexture.pixels().length == 0) {
                throw new IllegalStateException("Colormap texture at location " + textureLoc + " had invalid 0 dimension");
            }
        } else {
            ResourceLocation explTarget = colormap.getExplicitTargetTexture();
            if (explTarget != null) {
                Polytone.LOGGER.error("Could not resolve explicit texture at location {}.png. Skipping", explTarget);
            }
            if (strict) {
                throw new IllegalStateException("Could not find any colormap texture .png associated with path " + textureLoc + " for colormap '" + colormapName + "'");
            }
        }
    }

}
