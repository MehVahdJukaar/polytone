package net.mehvahdjukaar.polytone.block;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.colormap.IndexCompoundColorGetter;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.LegacyHelper;
import net.mehvahdjukaar.polytone.utils.PartialReloader;
import net.mehvahdjukaar.polytone.utils.PropertiesUtils;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
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
    private final Map<Block, List<BlockClientTickable>> particleAndSoundEmitters = new Object2ObjectOpenHashMap<>();

    public BlockPropertiesManager() {
        super("block_modifiers", "block_properties");
    }


    public record Resources(Map<ResourceLocation, JsonElement> jsons,
                            Map<ResourceLocation, ArrayImage> textures,
                            Map<ResourceLocation, Properties> ofProperties) {
    }

    @Override
    protected Resources prepare(ResourceManager resourceManager) {
        var jsons = this.getJsonsInDirectories(resourceManager);
        checkConditions(jsons);

        Map<ResourceLocation, ArrayImage> textures = new HashMap<>();

        Map<ResourceLocation, ArrayImage> ofTextures = ArrayImage.scanDirectory(resourceManager, "optifine/colormap");
        Map<ResourceLocation, ArrayImage> cmTextures = ArrayImage.scanDirectory(resourceManager, "colormatic/colormap");

        Map<ResourceLocation, Properties> ofProperties = PropertiesUtils.gatherProperties(resourceManager, "optifine/colormap");
        Map<ResourceLocation, JsonElement> ofJsons = new HashMap<>();
        scanDirectory(resourceManager, "optifine/colormap", GSON, ofJsons);
        checkConditions(ofJsons);

        ofJsons.forEach((k, v) -> ofProperties.put(k, PropertiesUtils.jsonToProperties(v)));

        textures.putAll(LegacyHelper.convertPaths(ofTextures));
        textures.putAll(LegacyHelper.convertPaths(cmTextures));

        textures.putAll(this.getImagesInDirectories(resourceManager));

        return new Resources(
                ImmutableMap.copyOf(jsons), ImmutableMap.copyOf(textures),
                ImmutableMap.copyOf(LegacyHelper.convertPaths(ofProperties)));
    }

    @Override
    protected void parseWithLevel(Resources resources, RegistryOps<JsonElement> ops, RegistryAccess access) {

        var jsons = resources.jsons();
        var textures = ArrayImage.groupTextures(resources.textures());
        var textureCopy = new HashMap<>(resources.textures);
        Set<ResourceLocation> usedTextures = new HashSet<>();

        Map<ResourceLocation, BlockPropertyModifier> parsedModifiers = new HashMap<>();
        parsedModifiers.putAll(LegacyHelper.convertBlockProperties(resources.ofProperties, textureCopy));
        parsedModifiers.putAll(LegacyHelper.convertInlinedPalettes(optifineColormapsToBlocks));

        LegacyHelper.convertOfBlockToFluidProp(parsedModifiers, textureCopy);
        LegacyHelper.convertOfBlockToDimensionProperties(parsedModifiers, textureCopy);


        // parse jsons
        for (var j : jsons.entrySet()) {
            JsonElement json = j.getValue();
            ResourceLocation id = j.getKey();


            BlockPropertyModifier prop = BlockPropertyModifier.CODEC.decode(ops, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.error("Could not decode Client Block Property with json id {} - error: {}", id, errorMsg))
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
            ResourceLocation id = entry.getKey();
            BlockPropertyModifier modifier = entry.getValue();

            if (!modifier.hasColormap() && textures.containsKey(id)) {
                //if this map doesn't have a colormap defined, we set it to the default impl IF there's a texture it can use
                var text = textures.get(id);
                IndexCompoundColorGetter defaultSampler = IndexCompoundColorGetter.createDefault(text.keySet(), true);
                modifier = modifier.merge(BlockPropertyModifier.ofBlockColor(defaultSampler));
            }

            //fill inline colormaps colormapTextures
            BlockColor tint = modifier.getColormap();
            ColormapsManager.tryAcceptingTextureGroup(textures, id, tint, usedTextures, true);

            addModifier(id, modifier);
        }

        textures.keySet().removeAll(usedTextures);

        // creates default modifiers for orphaned textures without one
        for (var entry : textures.entrySet()) {
            ResourceLocation id = entry.getKey();

            ArrayImage.Group image = entry.getValue();

            IndexCompoundColorGetter tintMap = IndexCompoundColorGetter.createDefault(image.keySet(), true);
            ColormapsManager.tryAcceptingTextureGroup(textures, id, tintMap, usedTextures, true);

            BlockPropertyModifier modifier = BlockPropertyModifier.ofBlockColor(tintMap);

            addModifier(id, modifier);
        }
    }


    private void addModifier(ResourceLocation fileId, BlockPropertyModifier mod) {
        for (Block block : mod.getTargets(fileId, BuiltInRegistries.BLOCK)) {
            modifiers.merge(block, mod, BlockPropertyModifier::merge);
        }
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        for (var e : vanillaProperties.entrySet()) {
            e.getValue().apply(e.getKey());
        }
        vanillaProperties.clear();
        modifiers.clear();
        optifineColormapsToBlocks.clear();
        particleAndSoundEmitters.clear();
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
        for (var e : modifiers.entrySet()) {
            Block target = e.getKey();

            BlockPropertyModifier value = e.getValue();
            vanillaProperties.put(target, value.apply(target));

            var particle = value.particleEmitters();
            particle.ifPresent(emitters -> particleAndSoundEmitters.computeIfAbsent(target, t -> new ArrayList<>())
                    .addAll(emitters));

            var sound = value.soundEmitters();
            sound.ifPresent(emitters -> particleAndSoundEmitters.computeIfAbsent(target, t -> new ArrayList<>())
                    .addAll(emitters));
        }
        if (!vanillaProperties.isEmpty()) {
            Polytone.LOGGER.info("Applied {} Custom Block Properties", vanillaProperties.size());
        }
        //clear as we dont need the anymore
        modifiers.clear();
    }

    //optifine stuff
    private final Map<ResourceLocation, String> optifineColormapsToBlocks = new HashMap<>();

    public void addSimpleColormap(ResourceLocation path, String str) {
        optifineColormapsToBlocks.put(path, str);
    }

    public void maybeEmitParticle(Block block, BlockState state, Level level, BlockPos pos) {
        var m = particleAndSoundEmitters.get(block);
        if (m != null) {
            for (var p : m) {
                p.tick(level, pos, state);
            }
        }
    }
}
