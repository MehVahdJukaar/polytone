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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

import static net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener.scanDirectory;

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

        //optifine stuff
        addOptifineColormaps(textures, usedTextures);

        // creates orphaned texture colormaps & properties
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            ResourceLocation id = t.getKey();
            Int2ObjectMap<ArrayImage> value = t.getValue();
            String path = id.getPath();


            //check if it has an optifine property

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
                Properties ofProp = resources.ofProperties.remove(id);
                if (ofProp != null) {
                    modifier = BlockPropertyModifier.fromOfProperties(ofProp);
                } else {
                    modifier = BlockPropertyModifier.ofColor(tintMap);
                }

                addModifier(id, modifier);
            }
        }

        //crap optifine single property shit. Just recolor a texture FFS
        for (var entry : resources.ofProperties.entrySet()) {
            var strayProp = entry.getValue();
            var modifier = BlockPropertyModifier.fromOfProperties(strayProp);
            if (modifier.tintGetter().get() instanceof Colormap c && !c.hasTexture()) {
                continue; //error basically. invalid one
            }
            ResourceLocation id = entry.getKey();
            addModifier(id, modifier);
            //and also tint them... shit format. just use block models and define the tint index
            var exp = modifier.explicitTargets();
            if (exp.isPresent()) {
                for (var t : exp.get()) {
                    BuiltInRegistries.BLOCK.getOptional(t)
                            .ifPresent(Polytone.VARIANT_TEXTURES::addTintOverrideHack);
                }
            }
        }
    }

    private void addOptifineColormaps(Map<ResourceLocation, Int2ObjectMap<ArrayImage>> textures,
                                      Set<ResourceLocation> usedTextures) {
        for (var special : optifineColormapsToBlocks.entrySet()) {
            ResourceLocation colormapId = special.getKey();
            var texture = textures.get(colormapId);
            if (texture != null) {
                addOptifineColormap(special.getValue(), texture.get(-1), colormapId, usedTextures);
            }
        }
    }

    private void addOptifineColormap(String specialTarget, ArrayImage image, ResourceLocation colormapId, Set<ResourceLocation> usedTexture) {
        Colormap colormap = Colormap.defTriangle();
        ColormapsManager.tryAcceptingTexture(image, colormapId, colormap, usedTexture);

        Set<ResourceLocation> targets = new HashSet<>();
        for (var name : specialTarget.split(" ")) {
            if (name.isEmpty()) continue;
            ResourceLocation blockId = new ResourceLocation(name);
            var b = BuiltInRegistries.BLOCK.getOptional(blockId);
            if (b.isPresent()) {
                Polytone.VARIANT_TEXTURES.addTintOverrideHack(b.get());
                targets.add(blockId);
            }
        }
        BlockPropertyModifier mod = BlockPropertyModifier.coloringBlocks(colormap, targets);
        if (!targets.isEmpty()) {
            addModifier(colormapId.withSuffix("_optifine"), mod);
        }
    }

    private void addModifier(ResourceLocation modifierId, BlockPropertyModifier mod) {
        var explTargets = mod.explicitTargets();
        //validate colormap
        if (mod.tintGetter().isPresent()) {
            if (mod.tintGetter().get() instanceof Colormap c && !c.hasTexture()) {
                throw new IllegalStateException("Did not find any texture png for implicit colormap for block modifier" + modifierId);
            }
        }
        Optional<Block> idTarget = BuiltInRegistries.BLOCK.getOptional(modifierId);
        if (explTargets.isPresent()) {
            if (idTarget.isPresent()) {
                Polytone.LOGGER.error("Found Block Properties Modifier with Explicit Targets ({}) also having a valid IMPLICIT Path Target ({})." +
                        "Consider moving it under your OWN namespace to avoid overriding other packs modifiers with the same path", explTargets.get(), modifierId);
            }
            for (var explicitId : explTargets.get()) {
                Optional<Block> target = BuiltInRegistries.BLOCK.getOptional(explicitId);
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
        optifineColormapsToBlocks.clear();
        particleEmitters.clear();
    }

    //optifine stuff
    private final Map<ResourceLocation, String> optifineColormapsToBlocks = new HashMap<>();

    public void addSimpleColormap(ResourceLocation path, String str) {
        optifineColormapsToBlocks.put(path, str);
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
