package net.mehvahdjukaar.polytone.content.shaders.light;

import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.common.expressions.impl.IEntityExp;
import net.mehvahdjukaar.polytone.common.expressions.impl.IParticleExp;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.compat.VeilCompat;
import net.mehvahdjukaar.polytone.content.shaders.GLHelper;
import net.mehvahdjukaar.polytone.content.shaders.IShader;
import net.mehvahdjukaar.polytone.content.shaders.IShaderModifier;
import net.mehvahdjukaar.polytone.content.config.ConfigsManager.ColoredLightsBackend;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ColoredLightsManager extends ContentManager<ColoredLightEntry> implements IShaderModifier {

    public record BlockRule(ColoredLight<IBlockExp> light, RuleTest predicate) {
        public boolean matches(BlockState state, net.minecraft.util.RandomSource random) {
            return predicate == AlwaysTrueTest.INSTANCE || predicate.test(state, random);
        }
    }

    private final Map<Block, List<BlockRule>> blocks = new IdentityHashMap<>();
    private final Map<EntityType<?>, ColoredLight<IEntityExp>> entities = new IdentityHashMap<>();
    private final Map<Item, ColoredLight<IEntityExp>> items = new IdentityHashMap<>();
    private final Map<ParticleType<?>, ColoredLight<IParticleExp>> particles = new IdentityHashMap<>();

    private final PolyShaderPointLights shaderPointLights = new PolyShaderPointLights();
    @Nullable
    private PointLightStorage pointLightStorage;
    private List<LightSource> lightSources = List.of();
    //read from chunk compile and async particle threds
    @Nullable
    private volatile BlockLightSource blockLights;
    @Nullable
    private volatile MovingLightSource<Particle, IParticleExp> particleLights;

    public ColoredLightsManager() {
        super(Spec.of("Colored Light", () -> ColoredLightEntry.CODEC)
                .folders("colored_lights")
                .wikiPage("Colored-Lights"));
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops,
                                  RegistryAccess access) {
        var jsons = resources.jsons();
        for (var e : parseEnabledJsons(jsons, ops)) {
            ResourceLocation fileId = e.getKey();
            switch (e.getValue()) {
                case ColoredLightEntry.Blocks b -> {
                    for (var holder : b.targets().compute(fileId, BuiltInRegistries.BLOCK.asLookup())) {
                        addBlockLight(holder.value(), b.light(), b.predicate());
                    }
                }
                case ColoredLightEntry.Entities en -> {
                    for (var holder : en.targets().compute(fileId, BuiltInRegistries.ENTITY_TYPE.asLookup())) {
                        addEntityLight(holder.value(), en.light());
                    }
                }
                case ColoredLightEntry.Items it -> {
                    for (var holder : it.targets().compute(fileId, BuiltInRegistries.ITEM.asLookup())) {
                        addItemLight(holder.value(), it.light());
                    }
                }
                case ColoredLightEntry.Particles p -> {
                    for (var holder : p.targets().compute(fileId, BuiltInRegistries.PARTICLE_TYPE.asLookup())) {
                        addParticleLight(holder.value(), p.light());
                    }
                }
            }
        }
    }

    /**
     * PUBLIC API FOR MODDERS BELOW HERE.
     * all else might change, these 4 methods should say stable
     */

    public void addBlockLight(Block block, ColoredLight<IBlockExp> light, RuleTest predicate) {
        blocks.computeIfAbsent(block, b -> new ArrayList<>()).add(new BlockRule(light, predicate));
    }

    public void addEntityLight(EntityType<?> type, ColoredLight<IEntityExp> light) {
        entities.put(type, light);
    }

    public void addItemLight(Item item, ColoredLight<IEntityExp> light) {
        items.put(item, light);
    }

    public void addParticleLight(ParticleType<?> type, ColoredLight<IParticleExp> light) {
        particles.put(type, light);
    }

    @Nullable
    public List<BlockRule> getBlockLights(Block block) {
        return blocks.get(block);
    }

    @Nullable
    public ColoredLight<IEntityExp> getEntityLight(EntityType<?> type) {
        return entities.get(type);
    }

    @Nullable
    public ColoredLight<IEntityExp> getItemLight(Item item) {
        return items.get(item);
    }

    @Nullable
    public ColoredLight<IParticleExp> getParticleLight(ParticleType<?> type) {
        return particles.get(type);
    }

    @Nullable
    public ColoredLight<IEntityExp> getLightFor(Entity entity) {
        if (entity instanceof ItemEntity item) {
            var light = items.get(item.getItem().getItem());
            if (light != null) return light;
        }
        var light = entities.get(entity.getType());
        if (light != null) return light;
        if (entity instanceof LivingEntity living) {
            for (InteractionHand hand : InteractionHand.values()) {
                var held = items.get(living.getItemInHand(hand).getItem());
                if (held != null) return held;
            }
        }
        return null;
    }

    public boolean hasBlockLights() {
        return !blocks.isEmpty();
    }

    public boolean hasEntityLights() {
        return !entities.isEmpty() || !items.isEmpty();
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
        // block rules stay even without Veil, the voxel volme reads them too
        if (blocks.isEmpty() && entities.isEmpty() && items.isEmpty() && particles.isEmpty()) return;
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

    @Nullable
    private PointLightStorage storageFor(ColoredLightsBackend backend) {
        return switch (backend) {
            case VEIL -> {
                if (!CompatHandler.VEIL) {
                    Polytone.LOGGER.info("Colored lights are set to use Veil but Veil is not installed. Install it or switch the backend to Built-in");
                    yield null;
                }
                yield VeilCompat.createPointLightStorage();
            }
            case HYBRID -> CompatHandler.VEIL ? VeilCompat.createPointLightStorage() : shaderPointLights;
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
    }

    public void onTick(ClientLevel level, BlockPos camera) {
        for (var source : lightSources) source.tick(level, camera.getCenter());
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

    @Override
    public List<String> getEnablingUniforms() {
        return List.of(PolyShaderPointLights.COUNT);
    }

    //the lightmap patched vanilla shaders get these from the include, not their json
    @Override
    public boolean isEnabledFor(IShader shader) {
        return GLHelper.usesUniform(shader.programId(), PolyShaderPointLights.COUNT);
    }

    @Override
    public void bindTo(IShader shader) {
        shaderPointLights.bindTo(shader);
    }
}
