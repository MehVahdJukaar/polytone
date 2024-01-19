package net.mehvahdjukaar.polytone.block;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.colormap.CompoundBlockColors;
import net.mehvahdjukaar.polytone.colormap.IColormapNumberProvider;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.mehvahdjukaar.polytone.utils.LegacyHelper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.StemBlock;

import java.util.*;

public class BlockPropertiesManager extends JsonImgPartialReloader {

    private final Map<Block, BlockPropertyModifier> vanillaProperties = new HashMap<>();

    // Block ID to modifier
    private final Map<Block, BlockPropertyModifier> modifiers = new HashMap<>();


    public BlockPropertiesManager() {
        super("block_properties");
    }

    @Override
    protected Resources prepare(ResourceManager resourceManager) {
        Map<ResourceLocation, JsonElement> jsons = new HashMap<>();
        scanDirectory(resourceManager, path(), GSON, jsons);

        Map<ResourceLocation, ArrayImage> textures = new HashMap<>();

        Map<ResourceLocation, ArrayImage> ofTextures = ArrayImage.gatherImages(resourceManager, "optifine/colormap");
        Map<ResourceLocation, ArrayImage> cmTextures = ArrayImage.gatherImages(resourceManager, "colormatic/colormap");

        textures.putAll(LegacyHelper.convertPaths(ofTextures));
        textures.putAll(LegacyHelper.convertPaths(cmTextures));

        textures.putAll(ArrayImage.gatherImages(resourceManager, path()));

        return new Resources(jsons, textures);
    }

    @Override
    public void process(Resources resources) {

        var jsons = resources.jsons();
        var textures = ArrayImage.groupTextures(resources.textures());

        Set<ResourceLocation> usedTextures = new HashSet<>();

        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            BlockPropertyModifier prop = BlockPropertyModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Client Block Property with json id {} - error: {}", id, errorMsg))
                    .getFirst();

            //fill inline colormaps colormapTextures
            var colormap = prop.tintGetter();
            if (colormap.isPresent() && colormap.get() instanceof CompoundBlockColors c) {
                ColormapsManager.fillCompoundColormapPalette(textures, id, c, usedTextures);
            }

            addModifier(id, prop);
        }

        // creates orphaned texture colormaps & properties
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            ResourceLocation id = t.getKey();
            Int2ObjectMap<ArrayImage> value = t.getValue();

            //optifine stuff
            String path = id.getPath();


            var special = colorPropertiesColormaps.get(id);
            if (special != null) {
                //TODO: improve tint assignment
                Colormap colormap = Colormap.defTriangle();
                ColormapsManager.tryAcceptingTexture(textures.get(id).get(-1), id, colormap, new HashSet<>());

                for (var name : special.split(" ")) {
                    try {
                        ResourceLocation blockId = new ResourceLocation(name);
                        var b = Registry.BLOCK.getOptional(blockId);
                        if (b.isPresent()) {
                            //TODO: merge
                            Polytone.VARIANT_TEXTURES.addTintOverrideHack(b.get());
                            addModifier(blockId, BlockPropertyModifier.ofColor(colormap));
                        }
                    } catch (Exception ignored) {
                    }
                }
                continue;
            }

            if (path.contains("stem")) {
                Colormap stemMap = Colormap.simple((state, level, pos) -> state.getValue(StemBlock.AGE) / 7f,
                        IColormapNumberProvider.ZERO);

                Colormap attachedMap = Colormap.simple(
                        IColormapNumberProvider.ONE, IColormapNumberProvider.ZERO);

                ColormapsManager.tryAcceptingTexture(textures.get(id).get(-1), id, stemMap, usedTextures);
                ColormapsManager.tryAcceptingTexture(textures.get(id).get(-1), id, attachedMap, usedTextures);

                // so stem maps to both
                if (!path.contains("melon")) {
                    addModifier(new ResourceLocation("pumpkin_stem"), BlockPropertyModifier.ofColor(stemMap));
                    addModifier(new ResourceLocation("attached_pumpkin_stem"), BlockPropertyModifier.ofColor(attachedMap));
                }
                if (!path.contains("pumpkin")) {
                    addModifier(new ResourceLocation("melon_stem"), BlockPropertyModifier.ofColor(stemMap));
                    addModifier(new ResourceLocation("attached_melon_stem"), BlockPropertyModifier.ofColor(attachedMap));
                }
            } else if (path.equals("redstone_wire")) {
                Colormap tintMap = Colormap.simple((state, level, pos) -> state.getValue(RedStoneWireBlock.POWER) / 15f,
                        IColormapNumberProvider.ZERO);

                ColormapsManager.tryAcceptingTexture(textures.get(id).get(-1), id, tintMap, usedTextures);

                addModifier(id, BlockPropertyModifier.ofColor(tintMap));
            } else {
                CompoundBlockColors tintMap = CompoundBlockColors.createDefault(value.keySet(), true);
                ColormapsManager.fillCompoundColormapPalette(textures, id, tintMap, usedTextures);

                addModifier(id, BlockPropertyModifier.ofColor(tintMap));
            }
        }
    }

    private void addModifier(ResourceLocation pathId, BlockPropertyModifier mod) {
        var explTargets = mod.explicitTargets();
        Optional<Block> idTarget = Registry.BLOCK.getOptional(pathId);
        if (explTargets.isPresent()) {
            if (idTarget.isPresent()) {
                Polytone.LOGGER.error("Found Block Properties Modifier with Explicit Targets ({}) also having a valid IMPLICIT Path Target ({})." +
                        "Consider moving it under your OWN namespace to avoid overriding other packs modifiers with the same path", explTargets.get(), pathId);
            }
            for (var explicitId : explTargets.get()) {
                Optional<Block> target = Registry.BLOCK.getOptional(explicitId);
                target.ifPresent(block -> modifiers.merge(block, mod, BlockPropertyModifier::merge));
            }
        }
        //no explicit targets. use its own ID instead
        else {
            idTarget.ifPresent(block -> modifiers.merge(block, mod, BlockPropertyModifier::merge));
        }
    }

    @Override
    public void apply() {
        for (var e : modifiers.entrySet()) {
            var block = e.getKey();

            BlockPropertyModifier value = e.getValue();
            vanillaProperties.put(block, value.apply(block));
        }
        if (!vanillaProperties.isEmpty())
            Polytone.LOGGER.info("Applied {} Custom Block Properties", vanillaProperties.size());

        modifiers.clear();
    }

    @Override
    public void reset() {
        for (var e : vanillaProperties.entrySet()) {
            e.getValue().apply(e.getKey());
        }
        vanillaProperties.clear();
        modifiers.clear();
        colorPropertiesColormaps.clear();
    }

    private final Map<ResourceLocation, String> colorPropertiesColormaps = new HashMap<>();

    public void addSimpleColormap(String path, String str) {
        colorPropertiesColormaps.put(new ResourceLocation(path), str);
    }
}
