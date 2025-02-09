package net.mehvahdjukaar.polytone.block;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.colormap.IndexCompoundColorGetter;
import net.mehvahdjukaar.polytone.utils.*;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener.scanDirectory;

public class BlockPropertiesManager extends PartialReloader<BlockPropertiesManager.Resources> {

    private final Map<Block, BlockPropertyModifier> vanillaProperties = new HashMap<>();

    // Block ID to modifier
    private final Map<Block, BlockPropertyModifier> modifiers = new HashMap<>();
    private final Map<Block, ClientTickModifier> particleAndSoundEmitters = new Object2ObjectOpenHashMap<>();

    public BlockPropertiesManager() {
        super("block_modifiers", "block_properties");
    }


    @Nullable
    public Vec3 maybeModify(BlockState state, BlockGetter blockGetter, BlockPos pos) {
        BlockPropertyModifier modifier = modifiers.get(state.getBlock());
        if (modifier != null) {
            var of = modifier.offsetType();
            if (of.isPresent()) {
                return of.get().evaluate(state, blockGetter, pos);
            }
        }
        return null;
    }


    public record Resources(Map<ResourceLocation, JsonElement> jsons,
                            Map<ResourceLocation, ArrayImage> textures,
                            Map<ResourceLocation, Properties> ofProperties) {
    }

    @Override
    protected Resources prepare(ResourceManager resourceManager) {
        var jsons = this.getJsonsInDirectories(resourceManager);

        Map<ResourceLocation, ArrayImage> textures = new HashMap<>();

        Map<ResourceLocation, ArrayImage> ofTextures = ArrayImage.scanDirectory(resourceManager, "optifine/colormap");
        Map<ResourceLocation, ArrayImage> cmTextures = ArrayImage.scanDirectory(resourceManager, "colormatic/colormap");

        Map<ResourceLocation, Properties> ofProperties = PropertiesUtils.gatherProperties(resourceManager, "optifine/colormap");
        Map<ResourceLocation, JsonElement> ofJsons = new HashMap<>();
        scanDirectory(resourceManager, "optifine/colormap", GSON, ofJsons);

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

        Map<ResourceLocation, Parsed<BlockPropertyModifier>> parsedModifiers = Utils.sortedMap();
        parsedModifiers.putAll(LegacyHelper.convertBlockProperties(resources.ofProperties, textureCopy));
        parsedModifiers.putAll(LegacyHelper.convertInlinedPalettes(optifineColormapsToBlocks));

        LegacyHelper.convertOfBlockToFluidProp(parsedModifiers, textureCopy);
        LegacyHelper.convertOfBlockToDimensionProperties(parsedModifiers, textureCopy);


        // parse jsons
        for (var j : jsons.entrySet()) {
            JsonElement json = j.getValue();
            ResourceLocation id = j.getKey();

            var prop = Parsed.parseOptionalOrPartial(BlockPropertyModifier.CODEC, BlockPropertyModifier.PARTIAL_CODEC,
                    json, ops, id, "block modifier");

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
            Parsed<BlockPropertyModifier> result = entry.getValue();
            BlockPropertyModifier modifier = result.getResultOrPartial();

            if (!modifier.hasColormap() && textures.containsKey(id)) {
                //if this map doesn't have a colormap defined, we set it to the default impl IF there's a texture it can use
                var text = textures.get(id);
                IndexCompoundColorGetter defaultSampler = IndexCompoundColorGetter.createDefault(text.keySet(), true);
                modifier = modifier.merge(BlockPropertyModifier.ofBlockColor(defaultSampler));
            }

            //fill inline colormaps colormapTextures
            BlockColor tint = modifier.getColormap();
            ColormapsManager.tryAcceptingTextureGroup(textures, id, tint, usedTextures, true);

            if (result.isEnabled()) addModifier(id, modifier);
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
        for (var block : mod.targets().compute(fileId, BuiltInRegistries.BLOCK)) {
            modifiers.merge(block.value(), mod, BlockPropertyModifier::merge);
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
            if (!particle.isEmpty()) {
                particleAndSoundEmitters.computeIfAbsent(target, t -> new ClientTickModifier()).addAll(particle);
            }
            var sound = value.soundEmitters();
            if (!sound.isEmpty()) {
                particleAndSoundEmitters.computeIfAbsent(target, t -> new ClientTickModifier()).addAll(sound);
            }

            if(value.disableParticles()){
                particleAndSoundEmitters.computeIfAbsent(target, t -> new ClientTickModifier()).cancelsExisting();
            }
        }
        if (!vanillaProperties.isEmpty()) {
            Polytone.LOGGER.info("Applied {} Block Modifiers", vanillaProperties.size());
        }
        //clear as we dont need the anymore
        modifiers.clear();
    }

    //optifine stuff
    private final Map<ResourceLocation, String> optifineColormapsToBlocks = new HashMap<>();

    public void addSimpleColormap(ResourceLocation path, String str) {
        optifineColormapsToBlocks.put(path, str);
    }

    public boolean maybeEmitParticle(Block block, BlockState state, Level level, BlockPos pos) {
        var m = particleAndSoundEmitters.get(block);
        if (m != null) {
            for (var p : m.tickables) {
                p.tick(level, pos, state);
            }
            return m.cancelExisting;
        }
        return false;
    }

    private static class ClientTickModifier{
        final List<BlockClientTickable> tickables = new ArrayList<>();
        boolean cancelExisting;

        public void add(BlockClientTickable tickable){
            tickables.add(tickable);
        }

        public void cancelsExisting(){
            cancelExisting = true;
        }

        public void addAll(List<? extends BlockClientTickable> emitters) {
            tickables.addAll(emitters);
        }
    }
}
