package net.mehvahdjukaar.polytone.content.shaders.light;

import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.api.IShaderInputs;
import net.mehvahdjukaar.polytone.api.IShaderInputPatch;
import net.mehvahdjukaar.polytone.api.IShaderSourcePatcher;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneShaderPatcher;
import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.server.packs.resources.Resource;
import net.mehvahdjukaar.polytone.content.config.ConfigsManager.ColoredLightsBackend;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.mehvahdjukaar.polytone.api.ResolvedPointLight;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ColoredLightsManager extends ContentManager<ColoredLightEntry> implements IShaderInputPatch, IShaderSourcePatcher {

    public record BlockRule(PointLightProvider.ForBlock light, RuleTest predicate) {
        public boolean matches(BlockState state, RandomSource random) {
            return predicate == AlwaysTrueTest.INSTANCE || predicate.test(state, random);
        }

        @Nullable
        public ResolvedPointLight resolve(BlockState state, BlockState lightState, Vec3 pos, ClientLevel level) {
            ResolvedPointLight resolved = light.resolve(lightState, pos, level);
            if (resolved == null || state == lightState) return resolved;
            //alias only copies the color!
            return new ResolvedPointLight(resolved.color(), state.getLightEmission(), resolved.brightness());
        }
    }

    private final Map<Block, List<BlockRule>> blocks = new IdentityHashMap<>();
    private final Map<EntityType<?>, PointLightProvider<Entity>> entities = new IdentityHashMap<>();
    private final Map<Item, PointLightProvider.ForItem> items = new IdentityHashMap<>();
    private final Map<ParticleType<?>, PointLightProvider<Particle>> particles = new IdentityHashMap<>();
    private final Map<BlockState, BlockState> blockStateAliases = new IdentityHashMap<>();
    private final Map<EntityType<?>, EntityType<?>> entityAliases = new IdentityHashMap<>();
    private final Map<Item, Item> itemAliases = new IdentityHashMap<>();
    private final Map<ParticleType<?>, ParticleType<?>> particleAliases = new IdentityHashMap<>();

    private final PolyShaderPointLights shaderPointLights = new PolyShaderPointLights();
    @Nullable
    private PointLightStorage pointLightStorage;
    private List<LightSource> lightSources = List.of();
    //read from chunk compile and async particle threds
    @Nullable
    private volatile BlockLightSource blockLights;
    @Nullable
    private volatile MovingLightSource<Particle> particleLights;
    private volatile Map<Block, Integer> textureLightColors = Map.of();

    public ColoredLightsManager() {
        super(Spec.of("Colored Light", () -> SchemaCodec.wrap(ColoredLightEntry.CODEC))
                .folders("colored_lights")
                .wikiPage("Colored-Lights"));
    }

    @Override
    protected AssetsFiles prepare(PreparableReloadListener.SharedState sharedState) {
        textureLightColors = Polytone.CONFIGS.textureLightColors.get() ? TextureLightColors.detect(sharedState.resourceManager()) : Map.of();
        return super.prepare(sharedState);
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops,
                                  HolderLookup.Provider access) {
        var jsons = resources.jsons();
        for (var e : parseEnabledJsons(jsons, ops)) {
            Identifier fileId = e.getKey();
            switch (e.getValue()) {
                case ColoredLightEntry.Blocks b -> {
                    for (var holder : b.targets().compute(fileId, BuiltInRegistries.BLOCK)) {
                        addBlockLight(holder.value(), b.light(), b.predicate());
                    }
                }
                case ColoredLightEntry.Entities en -> {
                    for (var holder : en.targets().compute(fileId, BuiltInRegistries.ENTITY_TYPE)) {
                        addEntityLight(holder.value(), en.light());
                    }
                }
                case ColoredLightEntry.Items it -> {
                    for (var holder : it.targets().compute(fileId, BuiltInRegistries.ITEM)) {
                        addItemLight(holder.value(), it.light());
                    }
                }
                case ColoredLightEntry.Particles p -> {
                    for (var holder : p.targets().compute(fileId, BuiltInRegistries.PARTICLE_TYPE)) {
                        addParticleLight(holder.value(), p.light());
                    }
                }
            }
        }
    }

    //mods go through VoxelVolumeApi. these are internals dont use!
    public void addBlockLight(Block block, PointLightProvider.ForBlock light, RuleTest predicate) {
        blocks.computeIfAbsent(block, b -> new ArrayList<>()).add(new BlockRule(light, predicate));
    }

    public void addEntityLight(EntityType<?> type, PointLightProvider<Entity> light) {
        entities.put(type, light);
    }

    public void addItemLight(Item item, PointLightProvider.ForItem light) {
        items.put(item, light);
    }

    public void addParticleLight(ParticleType<?> type, PointLightProvider<Particle> light) {
        particles.put(type, light);
    }

    public void addBlockAlias(Block block, Function<BlockState, BlockState> behavesAs) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            blockStateAliases.put(state, behavesAs.apply(state));
        }
    }

    public void addEntityAlias(EntityType<?> type, EntityType<?> behavesAs) {
        entityAliases.put(type, behavesAs);
    }

    public void addItemAlias(Item item, Item behavesAs) {
        itemAliases.put(item, behavesAs);
    }

    public void addParticleAlias(ParticleType<?> type, ParticleType<?> behavesAs) {
        particleAliases.put(type, behavesAs);
    }

    @Nullable
    public List<BlockRule> getBlockLights(Block block) {
        return blocks.get(block);
    }

    public BlockState aliasOf(BlockState state) {
        return blockStateAliases.getOrDefault(state, state);
    }

    public BlockState lightStateOf(BlockState state) {
        BlockState alias = blockStateAliases.get(state);
        if (alias == null || blocks.containsKey(state.getBlock())) return state;
        return alias;
    }

    @Nullable
    public PointLightProvider<Entity> getLightFor(Entity entity) {
        if (entity instanceof ItemEntity itemEntity) {
            var light = lightOfStack(itemEntity.getItem());
            if (light != null) return light;
        }
        var light = entities.get(entity.getType());
        if (light != null) return light;
        if (entity instanceof LivingEntity living) {
            for (InteractionHand hand : InteractionHand.values()) {
                var held = lightOfStack(living.getItemInHand(hand));
                if (held != null) return held;
            }
        }
        return null;
    }

    @Nullable
    private PointLightProvider<Entity> lightOfStack(ItemStack stack) {
        Item item = stack.getItem();
        var light = items.get(item);
        if (light == null) return null;
        return (holder, level) -> light.resolve(stack, holder, level);
    }

    public boolean hasBlockLights() {
        return !blocks.isEmpty();
    }

    public boolean hasEntityLights() {
        return !entities.isEmpty() || !items.isEmpty();
    }

    public boolean hasAnyLights() {
        return hasBlockLights() || hasEntityLights() || !particles.isEmpty();
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
        addTextureLights();
        flattenAliases();
        if (!hasAnyLights()) return;
        var backend = Polytone.CONFIGS.coloredLightsBackend.get();
        PointLightStorage storage = storageFor(backend);
        if (storage == null) return;
        pointLightStorage = storage;

        List<LightSource> sources = new ArrayList<>();
        //Otherwise we use ouw own stuff in the voxel grid
        if (hasBlockLights() && backend == ColoredLightsBackend.VEIL) {
            sources.add(blockLights = new BlockLightSource(storage));
        }
        if (hasEntityLights()) sources.add(MovingLightSource.entities(storage));
        if (!particles.isEmpty()) sources.add(particleLights = MovingLightSource.particles(storage));
        lightSources = sources;
    }

    //lowest priority, after packs and mod callbacks
    private void addTextureLights() {
        textureLightColors.forEach((block, color) -> {
            boolean alreadyLit = blocks.containsKey(block) || blockStateAliases.containsKey(block.defaultBlockState());
            if (alreadyLit) return;
            addBlockLight(block, (state, pos, level) -> state.getLightEmission() > 0 ?
                    new ResolvedPointLight(color, state.getLightEmission()) : null, AlwaysTrueTest.INSTANCE);
        });
    }

    private void flattenAliases() {
        entityAliases.forEach((type, behavesAs) -> copyLight(entities, type, behavesAs));
        itemAliases.forEach((item, behavesAs) -> copyLight(items, item, behavesAs));
        particleAliases.forEach((type, behavesAs) -> copyLight(particles, type, behavesAs));
        entityAliases.clear();
        itemAliases.clear();
        particleAliases.clear();
    }

    private static <K, V> void copyLight(Map<K, V> lights, K target, K behavesAs) {
        V light = lights.get(behavesAs);
        if (light != null) lights.putIfAbsent(target, light);
    }

    @Nullable
    private PointLightStorage storageFor(ColoredLightsBackend backend) {
        return switch (backend) {
            case VEIL -> {
                Polytone.LOGGER.info("Colored lights are set to use Veil but Veil is not available on this version. Switch the backend to Built-in");
                yield null;
            }
            case HYBRID -> shaderPointLights; //TODO: add later
            case BUILT_IN -> shaderPointLights;
            case OFF -> null;
        };
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        reset();
        for (var source : lightSources) source.clear();
        lightSources = List.of();
        if (pointLightStorage != null) pointLightStorage.clear();
        pointLightStorage = null;
        blockLights = null;
        particleLights = null;
    }

    private void reset() {
        blocks.clear();
        entities.clear();
        items.clear();
        particles.clear();
        blockStateAliases.clear();
        entityAliases.clear();
        itemAliases.clear();
        particleAliases.clear();
    }

    public void onTick(ClientLevel level, BlockPos camera) {
        for (var source : lightSources) source.tick(level, camera.getCenter());
    }

    public void beforeRenderLevel(Vec3 camera) {
        if (pointLightStorage == shaderPointLights) shaderPointLights.upload(camera);
    }

    public void close() {
        shaderPointLights.close();
    }

    public void onRenderTick(float partialTicks) {
        for (var source : lightSources) source.renderTick(partialTicks);
    }

    public void onParticlesCleared() {
        var lights = particleLights;
        if (lights != null) lights.dropTracked();
    }

    public void onParticleCreated(ParticleType<?> type, Particle particle) {
        var lights = particleLights;
        if (lights == null) return;
        var light = particles.get(type);
        if (light != null) lights.track(particle, light);
    }

    @Nullable
    public BlockLightSource.Scan openBlockScan() {
        var lights = blockLights;
        return lights != null ? lights.openScan() : null;
    }

    //face bit tracking needs to see every vertex shader, patched or not
    @Override
    public String patchShaderSource(Identifier shaderId, ShaderType type, String source, Map<Identifier, Resource> files) {
        if (type != ShaderType.VERTEX) return source;
        boolean canPatch = tintsVanillaBlockLight() && files.containsKey(PolytoneShaderPatcher.INCLUDE_LOCATION);
        return PolytoneShaderPatcher.onLoadVertexShader(shaderId, source, canPatch);
    }

    @Override
    public String patchSodiumChunkShaderSource(String source) {
        boolean canPatch = tintsVanillaBlockLight() && PolytoneShaderPatcher.isIncludeAvailable();
        return canPatch ? PolytoneShaderPatcher.patchSodiumChunkShader(source) : source;
    }

    //shaders can compile before our reload so this must not read pack data
    private static boolean tintsVanillaBlockLight() {
        return Polytone.CONFIGS.coloredLightsBackend.get().tintsVanillaBlockLight();
    }

    @Override
    public List<String> getEnablingUniforms() {
        return List.of(PolyShaderPointLights.BLOCK_NAME);
    }

    @Override
    public void bindToShader(IShaderInputs shader) {
        shaderPointLights.bindTo(shader);
    }
}
