package net.mehvahdjukaar.polytone.content.colormap;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.struc.ArrayImage;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.mehvahdjukaar.polytone.common.reloader.JsonImgPartialReloader;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.DryFoliageColor;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class ColormapsManager extends JsonImgPartialReloader {

    // Builtin colormaps
    //TODO: delegate to grass so we have quark compat
    public static final IColorGetter GRASS_COLOR = new IColorGetter.OfColorResolver((s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageGrassColor(l, p) : GrassColor.getDefaultColor(),
            BiomeColors.GRASS_COLOR_RESOLVER);

    public static final IColorGetter FOLIAGE_COLOR = new IColorGetter.OfColorResolver((s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageFoliageColor(l, p) : FoliageColor.get(0.5, 1.0),
            BiomeColors.FOLIAGE_COLOR_RESOLVER);

    public static final IColorGetter DRY_FOLIAGE_COLOR = new IColorGetter.OfColorResolver((s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageDryFoliageColor(l, p) : DryFoliageColor.get(0.5, 1.0),
            BiomeColors.DRY_FOLIAGE_COLOR_RESOLVER);

    public static final IColorGetter WATER_COLOR = new IColorGetter.OfColorResolver((s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageWaterColor(l, p) : 0xFF000000,
            BiomeColors.WATER_COLOR_RESOLVER);

    // custom defined colormaps
    private final MapRegistry<Supplier<IColorGetter>> colormaps = new MapRegistry<>("Polytone Colormaps");
    private final Map<IColorGetter, IColorGetter> concurrentColormaps = new HashMap<>();


    public Codec<IColorGetter> byNameCodec() {
        return colormaps.xmap(Supplier::get, s -> () -> s);
    }

    //dumb but better than codec madness since we have the supplier thing here
    public IColorGetter getOrCreateConcurrentColormap(IColorGetter colormap) {
        return concurrentColormaps.computeIfAbsent(colormap, IColorGetter::makeConcurrent);
    }

    public ColormapsManager() {
        super("colormaps");
    }

    @Override
    protected void parseWithLevel(Resources resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        //builtin stuff
        colormaps.register(Identifier.parse("grass_color"), () -> GRASS_COLOR);
        colormaps.register(Identifier.parse("foliage_color"), () -> FOLIAGE_COLOR);
        colormaps.register(Identifier.parse("dry_foliage_color"), () -> DRY_FOLIAGE_COLOR);
        colormaps.register(Identifier.parse("water_color"), () -> WATER_COLOR);
        //These create new incomplete ones every time
        colormaps.register(Identifier.parse("biome_sample"), Colormap::createDefSquare);
        colormaps.register(Identifier.parse("triangular_biome_sample"), Colormap::createDefTriangle);
        colormaps.register(Identifier.parse("fixed"), Colormap::createFixed);
        colormaps.register(Identifier.parse("grid"), Colormap::createBiomeId);
        colormaps.register(Identifier.parse("damage"), Colormap::createDamage);

        var jsons = resources.jsons();
        var textures = new HashMap<>(resources.textures());

        Set<Identifier> usedTextures = new HashSet<>();

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

        //initialize recursive stuff
        /*
        for (var c : colormaps.getValues()) {
            if (c.get() instanceof Colormap cm && cm.lazyFallback != null) {
                try {
                    cm.fallback = runCodec(ops, cm.lazyFallback);
                } catch (Exception e) {
                    Polytone.LOGGER.error("Failed to initialize colormap fallback", e);
                }
                cm.lazyFallback = null;
            }
        }*/


        // creates orphaned texture colormaps
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            Identifier id = t.getKey();
            Colormap defaultColormap = Colormap.createDefTriangle();
            defaultColormap.inlined = false;
            tryAcceptingTexture(textures, id, defaultColormap, usedTextures, true);
            // we need to fill these before we parse the properties as they will be referenced below
            add(id, defaultColormap);
        }
    }

    private <T> IColorGetter runCodec(DynamicOps o, Dynamic<T> dynamic) {
        DynamicOps<T> ops = (DynamicOps<T>) o;
        return this.byNameCodec().decode(ops, dynamic.getValue())
                .getOrThrow().getFirst();
    }


    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {

    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        colormaps.clear();
        concurrentColormaps.clear();
        PlatStuff.unregisterAllCustomColorResolves();
    }

    public void add(Identifier id, Colormap colormap) {
        colormaps.register(id, () -> colormap);
        if (colormap.needsToFillTexture()) {
            throw new IllegalStateException("Did not find any texture png for colormap " + id);
        }
    }


    //helper methods
    public static void tryAcceptingTextureGroup(Map<Identifier, ArrayImage.Group> availableTextures,
                                                Identifier defaultPath, BlockColor col, Set<Identifier> usedTexture, boolean strict) {
        if (col instanceof IColorGetter cg && !cg.needsToFillTexture()) {
            return;
        }
        if (col instanceof IndexCompoundColorGetter c) {
            tryAcceptingTextureGroup(availableTextures, defaultPath, c, usedTexture, strict);
        } else if (col instanceof Colormap c) {
            tryAcceptingTextureGroup(availableTextures, defaultPath, c, usedTexture, strict);
        }
    }

    private static void tryAcceptingTextureGroup(Map<Identifier, ArrayImage.Group> availableTextures,
                                                 Identifier defaultPath, Colormap c, Set<Identifier> usedTexture, boolean strict) {
        Identifier textureLoc = c.getTargetTexture(defaultPath);
        ArrayImage.Group group = availableTextures.get(textureLoc);
        ArrayImage texture = group != null ? group.getDefault() : null;
        tryAcceptingTexture(texture, textureLoc, c, usedTexture, strict);
    }

    private static void tryAcceptingTextureGroup(Map<Identifier, ArrayImage.Group> textures,
                                                 Identifier id, IndexCompoundColorGetter colormap,
                                                 Set<Identifier> usedTextures, boolean strict) {
        var blockColorGetters = colormap.getGetters();

        for (var g : blockColorGetters.int2ObjectEntrySet()) {
            int index = g.getIntKey();
            BlockColor inner = g.getValue();

            if (inner instanceof Colormap c && c.needsToFillTexture()) {

                var textureMap = textures.get(c.getTargetTexture(id));

                if (strict && textureMap == null) {
                    throw new IllegalStateException("Could not find a texture for tint index " + index + " for compound colormap " + id + "." +
                            "Expected " + id + "_" + index);
                }

                if (blockColorGetters.size() == 1 || index == 0) {
                    //try twice. first time doesn't throw
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

    public static void tryAcceptingTexture(Map<Identifier, ArrayImage> availableTextures,
                                           Identifier defaultPath,
                                           @Nullable Object col, Set<Identifier> usedTexture, boolean strict) {
        if (col instanceof Colormap colormap) {
            Identifier textureLoc = colormap.getTargetTexture(defaultPath);
            ArrayImage texture = availableTextures.get(textureLoc);
            tryAcceptingTexture(texture, textureLoc, colormap, usedTexture, strict);
            colormap.debugID = textureLoc;
        }
    }

    private static void tryAcceptingTexture(@Nullable ArrayImage selectedTexture, Identifier textureLoc, Colormap colormap,
                                            Set<Identifier> usedTexture, boolean strict) {
        if (!colormap.needsToFillTexture()) {
            return; //we already are filled
        }
        //hack. for inlined this will be the parent modifier id.
        String colormapName = colormap.inlined ? "Inlined Colormap from modifier " + textureLoc.toString() : "Colormap at " + textureLoc.toString();

        if (selectedTexture != null) {
            usedTexture.add(textureLoc);
            if (selectedTexture.pixels().length == 0) {
                throw new IllegalStateException("Colormap texture at location " + textureLoc + " had invalid 0 dimension");
            }
            colormap.acceptTexture(selectedTexture);
        } else {
            Identifier explTarget = colormap.getExplicitTargetTexture();
            if (explTarget != null) {
                Polytone.LOGGER.error("Could not resolve explicit texture at location {}.png. Skipping", explTarget);
            }
            if (strict) {
                throw new IllegalStateException("Could not find any colormap texture .png associated with path " + textureLoc + " for colormap '" + colormapName + "'");
            }
        }
    }

    public Collection<Identifier> getAllNames() {
        return colormaps.keySet();
    }

    @Nullable
    public IColorGetter get(String id) {
        var c = colormaps.getValue(id);
        return c != null ? c.get() : null;
    }
}
