package net.mehvahdjukaar.polytone.content.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.colormap.ColormapTextures;
import net.mehvahdjukaar.polytone.content.colormap.IndexCompoundColorGetter;
import net.mehvahdjukaar.polytone.companion.TrackedTextures;
import net.mehvahdjukaar.polytone.utils.*;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener.scanDirectory;

public class BlockPropertiesManager extends ContentManager<BlockPropertyModifier, AssetsFiles> {

    private final Map<Block, BlockPropertyModifier> vanillaProperties = new HashMap<>();

    // Block ID to modifier
    private final Map<Block, BlockPropertyModifier> modifiers = new HashMap<>();
    private final Map<Block, ClientTickModifier> particleAndSoundEmitters = new Object2ObjectOpenHashMap<>();

    private final Map<Block, Boolean> terrainParticleTintOverrides = new HashMap<>();

    //replacing vanilla color resolvers too for better mod compat
    private ColorResolver vanillaGrassColorResolver = null;
    private ColorResolver vanillaFoliageColorResolver = null;

    /** Optifine/Colormatic legacy .properties, scanned in prepare() and consumed in parseWithLevel(). */
    private Map<ResourceLocation, Properties> ofProperties = Map.of();

    public BlockPropertiesManager() {
        super(Spec.of("Block modifier", () -> SchemaCodec.wrap(BlockPropertyModifier.CODEC))
                .wikiPage("Block-Properties-Modifiers")
                .companions(ColormapTextures.groupedTexture(
                        (BlockPropertyModifier m) -> m.getColormap()))
                .folders("block_modifiers", "block_properties"));
    }


    @Nullable
    public Vec3 maybeModifyOffset(BlockState state, BlockGetter blockGetter, BlockPos pos) {
        BlockPropertyModifier modifier = modifiers.get(state.getBlock());
        if (modifier != null) {
            Optional<BlockBehaviour.OffsetFunction> of = modifier.offsetType();
            if (of.isPresent()) {
                return of.get().evaluate(state, blockGetter, pos);
            }
        }
        return null;
    }

    public boolean hasVisualOffset(BlockState state) {
        BlockPropertyModifier modifier = modifiers.get(state.getBlock());
        return modifier != null && modifier.offsetType().isPresent();
    }

    @Nullable
    public Boolean getTerrainTintOverride(Block block) {
        return terrainParticleTintOverrides.get(block);
    }


    @Override
    protected AssetsFiles prepare(ResourceManager resourceManager) {
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

        Map<ResourceLocation, ArrayImage> myTextures = this.getImagesInDirectories(resourceManager);
        textures.putAll(myTextures);

        this.ofProperties = ImmutableMap.copyOf(LegacyHelper.convertPaths(ofProperties));
        return new AssetsFiles(ImmutableMap.copyOf(jsons), ImmutableMap.copyOf(textures));
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, RegistryAccess access) {

        var jsons = resources.jsons();
        var textures = new TrackedTextures(resources.textures());
        var textureCopy = new HashMap<>(resources.textures());

        LinkedListMultimap<ResourceLocation, Parsed<BlockPropertyModifier>> parsedModifiers = LinkedListMultimap.create();
        LegacyHelper.convertBlockProperties(this.ofProperties, textureCopy).forEach(parsedModifiers::put);
        LegacyHelper.convertInlinedPalettes(optifineColormapsToBlocks).forEach(parsedModifiers::put);

        LegacyHelper.convertOfBlockToFluidProp(parsedModifiers, textureCopy);
        LegacyHelper.convertOfBlockToDimensionProperties(parsedModifiers, textureCopy);


        // parse jsons
        for (var j : jsons.entrySet()) {
            JsonElement json = j.getValue();
            ResourceLocation id = j.getKey();

            var prop = Parsed.parseOptionalOrPartial(
                    BlockPropertyModifier.CODEC, BlockPropertyModifier.PARTIAL_CODEC,
                    json, ops, id, "block modifier");
            parsedModifiers.put(id, prop);
        }
        //TODO:
        //parsedModifiers = Parsed.sortByPriority(parsedModifiers);


        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers.entries()) {
            ResourceLocation id = entry.getKey();
            Parsed<BlockPropertyModifier> result = entry.getValue();
            BlockPropertyModifier modifier = result.getResultOrPartial();

            if (!modifier.hasColormap()) {
                //if this map doesn't have a colormap defined, we set it to the default impl IF there's a texture it can use
                Set<Integer> indices = ColormapTextures.usableTintIndices(textures, id);
                if (!indices.isEmpty()) {
                    IndexCompoundColorGetter defaultSampler = IndexCompoundColorGetter.createDefault(indices, true);
                    modifier = modifier.merge(BlockPropertyModifier.ofBlockColor(defaultSampler));
                }
            }

            //fill inline colormaps colormapTextures
            ColormapTextures.fill(companions, textures, id, modifier, true);

            if (result.isEnabled()) addModifier(id, modifier);
        }

        // creates default modifiers for orphaned textures without one
        for (ResourceLocation id : ColormapTextures.orphanStems(textures)) {
            IndexCompoundColorGetter tintMap = IndexCompoundColorGetter.createDefault(
                    ColormapTextures.usableTintIndices(textures, id), true);
            BlockPropertyModifier modifier = BlockPropertyModifier.ofBlockColor(tintMap);
            ColormapTextures.fill(companions, textures, id, modifier, true);

            addModifier(id, modifier);
        }
    }


    private void addModifier(ResourceLocation fileId, BlockPropertyModifier mod) {
        for (var block : mod.targets().compute(fileId, BuiltInRegistries.BLOCK.asLookup())) {
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
        terrainParticleTintOverrides.clear();

        if (vanillaGrassColorResolver != null) {
            BiomeColors.GRASS_COLOR_RESOLVER = vanillaGrassColorResolver;
        }
        vanillaGrassColorResolver = null;
        if (vanillaFoliageColorResolver != null) {
            BiomeColors.FOLIAGE_COLOR_RESOLVER = vanillaFoliageColorResolver;
        }
        vanillaFoliageColorResolver = null;
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
        for (var modifierEntry : modifiers.entrySet()) {
            Block target = modifierEntry.getKey();

            BlockPropertyModifier modifier = modifierEntry.getValue();

            vanillaProperties.put(target, modifier.apply(target));

            var particle = modifier.particleEmitters();
            if (!particle.isEmpty()) {
                particleAndSoundEmitters.computeIfAbsent(target, t -> new ClientTickModifier()).addAll(particle);
            }
            var sound = modifier.soundEmitters();
            if (!sound.isEmpty()) {
                particleAndSoundEmitters.computeIfAbsent(target, t -> new ClientTickModifier()).addAll(sound);
            }

            if (modifier.disableParticles()) {
                particleAndSoundEmitters.computeIfAbsent(target, t -> new ClientTickModifier()).cancelsExisting();
            }


            Optional<Boolean> bool = modifier.breakingParticlesTinted();
            bool.ifPresent(aBoolean -> terrainParticleTintOverrides.put(target, aBoolean));
        }
        if (!vanillaProperties.isEmpty()) {
            Polytone.LOGGER.info("Applied {} Block Modifiers", vanillaProperties.size());
        }
        //clear as we dont need the anymore
        // modifiers.clear();
    }

    protected void maybeAssignToDefaultGrassAndFoliage(Block block, BlockColor color) {
        //TODO: this doesnt work with IndexCompoundColorGetter
        ColorResolver cc = null;
        if (color instanceof IndexCompoundColorGetter ic) {
            for (var e : ic.getGetters().int2ObjectEntrySet()) {
                if (e instanceof ColorResolver c) {
                    cc = c;
                    break;
                }
            }
        }else if(color instanceof ColorResolver c){
            cc = c;
        }
        if (block == Blocks.GRASS_BLOCK && cc != null) {
            vanillaGrassColorResolver = BiomeColors.GRASS_COLOR_RESOLVER;
            BiomeColors.GRASS_COLOR_RESOLVER = cc;
        } else if (block == Blocks.OAK_LEAVES && cc != null) {
            vanillaFoliageColorResolver = BiomeColors.FOLIAGE_COLOR_RESOLVER;
            BiomeColors.FOLIAGE_COLOR_RESOLVER = cc;
        }
    }

    //optifine stuff
    private final Map<ResourceLocation, String> optifineColormapsToBlocks = new HashMap<>();

    public void addSimpleColormap(ResourceLocation path, String str) {
        optifineColormapsToBlocks.put(path, str);
    }

    public boolean runTickers(BlockState state, Level level, BlockPos pos, TickSource source) {
        var m = particleAndSoundEmitters.get(state.getBlock());
        if (m != null) {
            for (var p : m.tickables) {
                p.tick(level, pos, state, source);
            }
            return m.cancelExisting;
        }
        return false;
    }

    private static class ClientTickModifier {
        final List<BlockClientTickable> tickables = new ArrayList<>();
        boolean cancelExisting;

        public void add(BlockClientTickable tickable) {
            tickables.add(tickable);
        }

        public void cancelsExisting() {
            cancelExisting = true;
        }

        public void addAll(List<? extends BlockClientTickable> emitters) {
            tickables.addAll(emitters);
        }
    }
}
