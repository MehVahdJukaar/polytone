package net.mehvahdjukaar.polytone.block;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.colormap.CompoundBlockColors;
import net.mehvahdjukaar.polytone.colormap.IColormapNumberProvider;
import net.mehvahdjukaar.polytone.particle.ParticleEmitter;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.LegacyHelper;
import net.mehvahdjukaar.polytone.utils.PartialReloader;
import net.mehvahdjukaar.polytone.utils.PropertiesUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class BlockPropertiesManager extends PartialReloader<BlockPropertiesManager.Resources> {

    private final Map<Block, BlockPropertyModifier> vanillaProperties = new HashMap<>();

    // Block ID to modifier
    private final Map<Block, BlockPropertyModifier> modifiers = new HashMap<>();
    private final Map<Block, List<ParticleEmitter>> particleEmitters = new Object2ObjectOpenHashMap<>();

    public BlockPropertiesManager() {
        super("block_properties");
    }

    public record Resources(Map<ResourceLocation, JsonElement> jsons,
                            Map<ResourceLocation, ArrayImage> textures,
                            Map<ResourceLocation, Properties> ofProperties) {
    }

    @Override
    protected Resources prepare(ResourceManager resourceManager) {
        Map<ResourceLocation, JsonElement> jsons = new HashMap<>();
        scanDirectory(resourceManager, path(), GSON, jsons);

        Map<ResourceLocation, ArrayImage> textures = new HashMap<>();

        Map<ResourceLocation, ArrayImage> ofTextures = ArrayImage.gatherImages(resourceManager, "optifine/colormap");
        Map<ResourceLocation, ArrayImage> cmTextures = ArrayImage.gatherImages(resourceManager, "colormatic/colormap");

        Map<ResourceLocation, Properties> ofProperties = PropertiesUtils.gatherProperties(resourceManager, "optifine/colormap");

        textures.putAll(LegacyHelper.convertPaths(ofTextures));
        textures.putAll(LegacyHelper.convertPaths(cmTextures));

        textures.putAll(ArrayImage.gatherImages(resourceManager, path()));

        return new Resources(jsons, textures, LegacyHelper.convertPaths(ofProperties));
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

            var colormap = prop.tintGetter();
            if (colormap.isEmpty()) {
                //if this map doesn't have a colormap defined, we set it to the default impl IF there's a texture it can use
                var text = textures.get(id);
                if (text != null) {
                    CompoundBlockColors defaultSampler = CompoundBlockColors.createDefault(text.keySet(), true);
                    prop = prop.merge(BlockPropertyModifier.ofColor(defaultSampler));
                    colormap = prop.tintGetter();
                }
            }

            //fill inline colormaps colormapTextures
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

            //check it it has an optifine property

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

                BlockPropertyModifier modifier;
                Properties ofProp = resources.ofProperties.get(id);
                if (ofProp != null) {
                    modifier = BlockPropertyModifier.fromOfProperties(ofProp);
                } else {
                    modifier = BlockPropertyModifier.ofColor(tintMap);
                }

                addModifier(id, modifier);
            }
        }
    }

    private void addModifier(ResourceLocation pathId, BlockPropertyModifier mod) {
        var explTargets = mod.explicitTargets();
        //validate colormap
        if (mod.tintGetter().isPresent()) {
            if (mod.tintGetter().get() instanceof Colormap c && !c.hasTexture()) {
                throw new IllegalStateException("Did not find any texture png for implicit colormap for block modifier" + pathId);
            }
        }
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

            var particle = value.particleEmitters();
            particle.ifPresent(emitters -> particleEmitters.put(block, emitters));
        }
        if (!vanillaProperties.isEmpty())
            Polytone.LOGGER.info("Applied {} Custom Block Properties", vanillaProperties.size());

        //clear as we dont need the anymore
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
        particleEmitters.clear();
    }

    //optifine stuff
    private final Map<ResourceLocation, String> colorPropertiesColormaps = new HashMap<>();

    public void addSimpleColormap(String path, String str) {
        colorPropertiesColormaps.put(new ResourceLocation(path), str);
    }

    public void maybeEmitParticle(Block block, BlockState state, Level level, BlockPos pos) {
        var m = particleEmitters.get(block);
        if (m != null) {
            for (var p : m) {
                p.tick(level, pos, state);
            }
        }
    }
}
