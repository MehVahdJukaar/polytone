package net.mehvahdjukaar.polytone.colormap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
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
            l != null && p != null ? BiomeColors.getAverageGrassColor(l, p) : GrassColor.getDefaultColor();

    public static final BlockColor FOLIAGE_COLOR = (s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageFoliageColor(l, p) : FoliageColor.getDefaultColor();

    public static final BlockColor WATER_COLOR = (s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageWaterColor(l, p) : 0xFF000000;

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
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Colormap with json id " + id + "\n error: " + errorMsg))
                    .getFirst();

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
    }

    @Nullable
    public BlockColor get(ResourceLocation id) {
        //default samplers
        if (id.equals(new ResourceLocation("biome_sample"))) {
            return Colormap.defSquare();
        } else if (id.equals(new ResourceLocation("triangular_biome_sample"))) {
            return Colormap.defTriangle();
        } else if (id.equals(new ResourceLocation("fixed"))) {
            return Colormap.fixed();
        } else if (id.equals(new ResourceLocation("biome_id"))) {
            return Colormap.biomeId();
        }
        return colormapsIds.get(id);
    }

    @Nullable
    public ResourceLocation getKey(BlockColor object) {
        return colormapsIds.inverse().get(object);
    }


    public void add(ResourceLocation id, Colormap colormap) {
        colormapsIds.put(id, colormap);
        if (!colormap.hasTexture()) {
            throw new IllegalStateException("Did not find any texture png for colormap " + id);
        }
    }


    public static void fillCompoundColormapPalette(Map<ResourceLocation, ArrayImage.Group> textures,
                                                   ResourceLocation id, CompoundBlockColors colormap,
                                                   Set<ResourceLocation> usedTextures) {
        var getters = colormap.getGetters();

        for (var g : getters.int2ObjectEntrySet()) {
            int index = g.getIntKey();
            BlockColor inner = g.getValue();

            if (inner instanceof Colormap c && !c.hasTexture()) {

                var textureMap = textures.get(c.getTargetTexture() == null ? id : c.getTargetTexture());

                if (textureMap != null) {
                    if (getters.size() == 1 || index == 0) {
                        try {
                            //try twice. first time doesnt throw
                            tryAcceptingTexture(textureMap.getDefault(), id, c, usedTextures);
                            continue;
                        } catch (Exception ignored) {
                        }
                        try {
                            tryAcceptingTexture(textureMap.get(index), id, c, usedTextures);
                        } catch (Exception e) {
                            throw new IllegalStateException("Failed applying a texture for tint index " + index + ": ", e);
                        }
                    }
                }
            }
        }

    }

    //helper method
    public static void tryAcceptingTexture(@Nullable ArrayImage texture, ResourceLocation textureLocation,
                                           Colormap colormap, Set<ResourceLocation> usedTexture) {
        if (colormap.hasTexture()) return; //we already are filled
        if (texture != null) {
            usedTexture.add(textureLocation);
            colormap.acceptTexture(texture);
            if (texture.pixels().length == 0) {
                throw new IllegalStateException("Colormap texture at location " + textureLocation + " had invalid 0 dimension");
            }
        } else
            throw new IllegalStateException("Could not find any colormap texture .png associated with path " + textureLocation);
    }


}
