package net.mehvahdjukaar.polytone.content.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.LegacyHelper;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.reloader.PartialReloader;
import net.mehvahdjukaar.polytone.common.struc.ArrayImage;
import net.mehvahdjukaar.polytone.common.struc.PropertiesUtils;
import net.mehvahdjukaar.polytone.content.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.content.colormap.IndexCompoundColorGetter;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BlockPropertiesManager extends PartialReloader<BlockPropertiesManager.Resources> {

    private final Map<Block, BlockPropertyModifier> vanillaProperties = new HashMap<>();

    // Block ID to modifier
    private final Map<Block, BlockPropertyModifier> modifiers = new HashMap<>();
    private final Map<Block, ClientTickModifier> particleAndSoundEmitters = new Object2ObjectOpenHashMap<>();

    private final Map<Block, Boolean> terrainParticleTintOverrides = new HashMap<>();

    //replacing vanilla color resolvers too for better mod compat
    private ColorResolver vanillaGrassColorResolver = null;
    private ColorResolver vanillaFoliageColorResolver = null;

    public BlockPropertiesManager() {
        super("block_modifiers", "block_properties");
    }


    @Nullable
    public Vec3 maybeModifyOffset(BlockState state, BlockPos pos) {
        BlockPropertyModifier modifier = modifiers.get(state.getBlock());
        if (modifier != null) {
            Optional<BlockBehaviour.OffsetFunction> of = modifier.offsetType();
            if (of.isPresent()) {
                return of.get().evaluate(state, pos);
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

    public record Resources(Map<Identifier, JsonElement> jsons,
                            Map<Identifier, ArrayImage> textures,
                            Map<Identifier, Properties> ofProperties) {

    }

    @Override
    protected Resources prepare(PreparableReloadListener.SharedState sharedState) {
        var resourceManager = sharedState.resourceManager();
        var jsons = this.getJsonsInDirectories(resourceManager);

        boolean legacyParsing = Polytone.CONFIGS.legacyParsing.get();
        Map<Identifier, ArrayImage> textures = new HashMap<>();
        Map<Identifier, Properties> ofProperties = new HashMap<>();
        if (legacyParsing) {
            Map<Identifier, ArrayImage> ofTextures = ArrayImage.scanDirectory(resourceManager, "optifine/colormap");
            Map<Identifier, ArrayImage> cmTextures = ArrayImage.scanDirectory(resourceManager, "colormatic/colormap");

            ofProperties = PropertiesUtils.gatherProperties(resourceManager, "optifine/colormap");
            Map<Identifier, JsonElement> ofJsons = new HashMap<>();
            scanDirectory(resourceManager, "optifine/colormap", GSON, ofJsons);

            for(var j : ofJsons.entrySet()){
                ofProperties.put(j.getKey(), PropertiesUtils.jsonToProperties(j.getValue()));
            }

            textures.putAll(LegacyHelper.convertPaths(ofTextures));
            textures.putAll(LegacyHelper.convertPaths(cmTextures));
        }

        Map<Identifier, ArrayImage> myTextures = this.getImagesInDirectories(resourceManager);
        textures.putAll(myTextures);

        return new Resources(
                ImmutableMap.copyOf(jsons), ImmutableMap.copyOf(textures),
                ImmutableMap.copyOf(LegacyHelper.convertPaths(ofProperties)));
    }

    @Override
    protected void parseWithLevel(Resources resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {

        var jsons = resources.jsons();
        var textures = ArrayImage.groupTextures(resources.textures());
        var textureCopy = new HashMap<>(resources.textures);
        Set<Identifier> usedTextures = new HashSet<>();

        LinkedListMultimap<Identifier, Parsed<BlockPropertyModifier>> parsedModifiers = LinkedListMultimap.create();
        LegacyHelper.convertBlockProperties(resources.ofProperties, textureCopy).forEach(parsedModifiers::put);
        LegacyHelper.convertInlinedPalettes(optifineColormapsToBlocks).forEach(parsedModifiers::put);

        LegacyHelper.convertOfBlockToFluidProp(parsedModifiers, textureCopy);
        LegacyHelper.convertOfBlockToDimensionProperties(parsedModifiers, textureCopy);


        // parse jsons
        var parsedJsons = Parsed.batchParseOrPartial(jsons,
                BlockPropertyModifier.CODEC, BlockPropertyModifier.PARTIAL_CODEC,
                ops, "block modifier");
        for (var entry : parsedJsons.entrySet()) {
            parsedModifiers.put(entry.getKey(), entry.getValue());
        }


        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers.entries()) {
            Identifier id = entry.getKey();
            Parsed<BlockPropertyModifier> result = entry.getValue();
            BlockPropertyModifier modifier = result.getResultOrPartial();

            if (!modifier.hasColormap() && textures.containsKey(id)) {
                //if this map doesn't have a colormap defined, we set it to the default impl IF there's a texture it can use
                var text = textures.get(id);
                IndexCompoundColorGetter defaultSampler = IndexCompoundColorGetter.createDefault(text.keySet(), true);
                modifier = modifier.merge(BlockPropertyModifier.ofBlockColor(defaultSampler));
            }

            //fill inline colormaps colormapTextures
            IColorGetter tint = modifier.getColormap();
            ColormapsManager.tryAcceptingTextureGroup(textures, id, tint, usedTextures, true);

            if (result.isEnabled()) addModifier(id, modifier);
        }

        textures.keySet().removeAll(usedTextures);

        // creates default modifiers for orphaned textures without one
        for (var entry : textures.entrySet()) {
            Identifier id = entry.getKey();

            ArrayImage.Group image = entry.getValue();

            IndexCompoundColorGetter tintMap = IndexCompoundColorGetter.createDefault(image.keySet(), true);
            ColormapsManager.tryAcceptingTextureGroup(textures, id, tintMap, usedTextures, true);

            BlockPropertyModifier modifier = BlockPropertyModifier.ofBlockColor(tintMap);

            addModifier(id, modifier);
        }
    }


    private void addModifier(Identifier fileId, BlockPropertyModifier mod) {
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
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
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

    protected void maybeAssignToDefaultGrassAndFoliage(Block block, IColorGetter color) {
        //TODO: this doesnt work with IndexCompoundColorGetter
        ColorResolver cc = null;
        if (color instanceof IndexCompoundColorGetter ic) {
            for (var e : ic.getGetters().int2ObjectEntrySet()) {
                if (e instanceof ColorResolver c) {
                    cc = c;
                    break;
                }
            }
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

    private final Map<Identifier, String> optifineColormapsToBlocks = new HashMap<>();

    public void addSimpleColormap(Identifier path, String str) {
        optifineColormapsToBlocks.put(path, str);
    }

    public boolean runTickers(BlockState state, ClientLevel level, BlockPos pos, TickSource source) {
        ClientTickModifier m = particleAndSoundEmitters.get(state.getBlock());
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

    //TODO: add this
    public void maybeSpawnBreakParticles(BlockState state, ClientLevel level, BlockPos pos, Direction direction) {
        if (true) return;
        var m = particleAndSoundEmitters.get(state.getBlock());

        RandomSource random = level.getRandom();
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        float f = 0.1F;
        AABB aABB = state.getShape(level, pos).bounds();
        double d = (double) i + random.nextDouble() * (aABB.maxX - aABB.minX - 0.20000000298023224) + 0.10000000149011612 + aABB.minX;
        double e = (double) j + random.nextDouble() * (aABB.maxY - aABB.minY - 0.20000000298023224) + 0.10000000149011612 + aABB.minY;
        double g = (double) k + random.nextDouble() * (aABB.maxZ - aABB.minZ - 0.20000000298023224) + 0.10000000149011612 + aABB.minZ;
        if (direction == Direction.DOWN) {
            e = (double) j + aABB.minY - 0.10000000149011612;
        }

        if (direction == Direction.UP) {
            e = (double) j + aABB.maxY + 0.10000000149011612;
        }

        if (direction == Direction.NORTH) {
            g = (double) k + aABB.minZ - 0.10000000149011612;
        }

        if (direction == Direction.SOUTH) {
            g = (double) k + aABB.maxZ + 0.10000000149011612;
        }

        if (direction == Direction.WEST) {
            d = (double) i + aABB.minX - 0.10000000149011612;
        }

        if (direction == Direction.EAST) {
            d = (double) i + aABB.maxX + 0.10000000149011612;
        }
    }
}
