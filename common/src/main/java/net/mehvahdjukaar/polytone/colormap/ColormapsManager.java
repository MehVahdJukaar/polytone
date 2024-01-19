package net.mehvahdjukaar.polytone.colormap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ColormapsManager extends JsonImgPartialReloader {

    // Builtin colormaps
    //TODO: delegate to grass so we have quark compat
    public static final BlockColor GRASS_COLOR = (s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageGrassColor(l, p) :  GrassColor.get(0.5, 1.0);

    public static final BlockColor FOLIAGE_COLOR = (s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageFoliageColor(l, p) : FoliageColor.getDefaultColor();

    public static final BlockColor WATER_COLOR = (s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageWaterColor(l, p) : 0xFF000000;

    public static final BlockColor BIOME_SAMPLE = Colormap.defSquare();

    public static final BlockColor TR_BIOME_SAMPLE = Colormap.defTriangle();

    // custom defined colormaps
    private final BiMap<ResourceLocation, BlockColor> colormapsIds = HashBiMap.create();

    public ColormapsManager() {
        super("colormaps");
    }


    @Override
    public void process(Resources resources) {
        var jsons = resources.jsons();
        var textures = resources.textures();

        Set<ResourceLocation> usedTextures = new HashSet<>();

        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            Colormap colormap = Colormap.DIRECT_CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Colormap with json id {} - error: {}",
                            id, errorMsg)).getFirst();
            tryAcceptingTexture(textures.get(id), id, colormap, usedTextures);
            // we need to fill these before we parse the properties as they will be referenced below
            add(id, colormap);
        }


        // creates orphaned texture colormaps
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            ResourceLocation id = t.getKey();
            Colormap defaultColormap = Colormap.defTriangle();
            tryAcceptingTexture(textures.get(id), id, defaultColormap, usedTextures);
            // we need to fill these before we parse the properties as they will be referenced below
            add(id, defaultColormap);
        }
    }

    @Override
    public void reset() {
        colormapsIds.clear();
        colormapsIds.put(new ResourceLocation("grass_color"), GRASS_COLOR);
        colormapsIds.put(new ResourceLocation("foliage_color"), FOLIAGE_COLOR);
        colormapsIds.put(new ResourceLocation("water_color"), WATER_COLOR);
        colormapsIds.put(new ResourceLocation("biome_sample"), BIOME_SAMPLE);
        colormapsIds.put(new ResourceLocation("triangular_biome_sample"), TR_BIOME_SAMPLE);
    }

    @Nullable
    public BlockColor get(ResourceLocation id) {
        return colormapsIds.get(id);
    }

    @Nullable
    public ResourceLocation getKey(BlockColor object) {
        return colormapsIds.inverse().get(object);
    }


    public void add(ResourceLocation id, Colormap colormap) {
        colormapsIds.put(id, colormap);
    }


    public static void fillCompoundColormapPalette(Map<ResourceLocation, Int2ObjectMap<ArrayImage>> textures,
                                                   ResourceLocation id, CompoundBlockColors colormap, Set<ResourceLocation> usedTextures) {
        var getters = colormap.getGetters();

        var textureMap = textures.get(id);

        if (textureMap != null) {
            for (var g : getters.int2ObjectEntrySet()) {
                int index = g.getIntKey();
                BlockColor inner = g.getValue();
                if (inner instanceof Colormap c && !c.isReference) {
                    boolean success = false;
                    if (getters.size() == 1 || index == 0) {
                        success = tryAcceptingTexture(textureMap.get(-1), id, c, usedTextures);
                    }
                    if (!success) {
                        success = tryAcceptingTexture(textureMap.get(index), id, c, usedTextures);
                    }
                    if (!success) {
                        throw new IllegalStateException("Could not find any colormap associated with " + id + " for tint index " + index + ". " +
                                "Expected: " + id);
                    }
                }
            }
        } else {
            throw new IllegalStateException("Could not find any colormap associated with " + id + ". " +
                    "Expected: " + id);
        }
    }

    //helper method
    public static boolean tryAcceptingTexture(@Nullable ArrayImage texture, ResourceLocation path,
                                              Colormap colormap, Set<ResourceLocation> usedTexture) {
        if (texture != null) {
            usedTexture.add(path);
            colormap.acceptTexture(texture);
            if (texture.pixels().length == 0) {
                throw new IllegalStateException("Colormap at location " + path + " had invalid 0 dimension");
            }
            return true;
        }
        return false;
    }


}
