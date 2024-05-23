package net.mehvahdjukaar.polytone.block;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.colormap.CompoundBlockColors;
import net.mehvahdjukaar.polytone.particle.ParticleEmitter;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.LegacyHelper;
import net.mehvahdjukaar.polytone.utils.PartialReloader;
import net.mehvahdjukaar.polytone.utils.PropertiesUtils;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
        checkConditions(jsons);

        Map<ResourceLocation, ArrayImage> textures = new HashMap<>();

        Map<ResourceLocation, ArrayImage> ofTextures = ArrayImage.gatherImages(resourceManager, "optifine/colormap");
        Map<ResourceLocation, ArrayImage> cmTextures = ArrayImage.gatherImages(resourceManager, "colormatic/colormap");

        Map<ResourceLocation, Properties> ofProperties = PropertiesUtils.gatherProperties(resourceManager, "optifine/colormap");
        Map<ResourceLocation, JsonElement> ofJsons = new HashMap<>();
        scanDirectory(resourceManager, "optifine/colormap", GSON, ofJsons);
        checkConditions(ofJsons);

        ofJsons.forEach((k, v) -> ofProperties.put(k, PropertiesUtils.jsonToProperties(v)));

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

        Map<ResourceLocation, BlockPropertyModifier> parsedModifiers = new HashMap<>();
        parsedModifiers.putAll(LegacyHelper.convertBlockProperties(resources.ofProperties, resources.textures));
        parsedModifiers.putAll(LegacyHelper.convertInlinedPalettes(optifineColormapsToBlocks));

        LegacyHelper.convertOfBlockToFluidProp(parsedModifiers, resources.textures);


        // parse jsons
        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            BlockPropertyModifier prop = BlockPropertyModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Client Block Property with json id {} - error: {}", id, errorMsg))
                    .getFirst();

            //always have priority
            if (parsedModifiers.containsKey(id)) {
                Polytone.LOGGER.warn("Found duplicate block modifier with id {}. This is likely a non .json converted legacy one" +
                        "Overriding previous one", id);
            }
            parsedModifiers.put(id, prop);
        }


        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers.entrySet()) {
            var id = entry.getKey();
            var modifier = entry.getValue();

            var colormap = modifier.tintGetter();
            if (colormap.isEmpty()) {
                //if this map doesn't have a colormap defined, we set it to the default impl IF there's a texture it can use

                var text = textures.get(id);
                if (text != null) {
                    CompoundBlockColors defaultSampler = CompoundBlockColors.createDefault(text.keySet(), true);
                    modifier = modifier.merge(BlockPropertyModifier.ofColor(defaultSampler));
                    colormap = modifier.tintGetter();
                }
            }

            //fill inline colormaps colormapTextures
            if (colormap.isPresent()) {
                BlockColor tint = colormap.get();
                if (tint instanceof CompoundBlockColors c) {
                    ColormapsManager.fillCompoundColormapPalette(textures, id, c, usedTextures);
                } else if (tint instanceof Colormap c) {
                    var text = textures.get(c.getTargetTexture() == null ? id : c.getTargetTexture());
                    if (text != null) {
                        ColormapsManager.tryAcceptingTexture(text.getDefault(), id, c, usedTextures);
                    } else if (c.getTargetTexture() != null) {
                        Polytone.LOGGER.error("Could not resolve explicit texture at location {}.png for colormap from block modifier {}. Skipping", c.getTargetTexture(), id);
                        continue;
                    }
                }
            }

            addModifier(id, modifier);
        }

        textures.keySet().removeAll(usedTextures);

        // creates default modifiers for orphaned textures without one
        for (var entry : textures.entrySet()) {
            ResourceLocation id = entry.getKey();

            ArrayImage.Group image = entry.getValue();

            CompoundBlockColors tintMap = CompoundBlockColors.createDefault(image.keySet(), true);
            ColormapsManager.fillCompoundColormapPalette(textures, id, tintMap, usedTextures);

            BlockPropertyModifier modifier = BlockPropertyModifier.ofColor(tintMap);

            addModifier(id, modifier);
        }
    }


    private void addModifier(ResourceLocation modifierId, BlockPropertyModifier mod) {
        var explTargets = mod.explicitTargets();
        //validate colormap
        if (mod.tintGetter().isPresent()) {
            if (mod.tintGetter().get() instanceof Colormap c && !c.hasTexture()) {
                Polytone.LOGGER.error("Did not find any texture png for implicit colormap from block modifier {}. Skipping", modifierId);
            }
        }
        Optional<Block> implicitTarget = BuiltInRegistries.BLOCK.getOptional(modifierId);
        if (explTargets.isPresent()) {
            if (implicitTarget.isPresent() && !explTargets.get().contains(modifierId)) {
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
            implicitTarget.ifPresent(block -> modifiers.merge(block, mod, BlockPropertyModifier::merge));
            if (implicitTarget.isEmpty()) {
                if (PlatStuff.isModLoaded(modifierId.getNamespace())) {
                    Polytone.LOGGER.error("Found Block Properties Modifier with no implicit target (expected block with ID {}) and no explicit targets. Skipping", modifierId);
                }
            }
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
