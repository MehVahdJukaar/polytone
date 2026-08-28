//veil has no 26.1 build yet so this whole thing is parked. uncomment it, the mixin json entries
//and the modifier `colored_light` sugar once veil ships one
/*
package net.mehvahdjukaar.polytone.content.light;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.IEntityExp;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.IParticleExp;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
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

public class ColoredLightsManager extends JsonPartialReloader<ColoredLightEntry> {

    public record BlockRule(ColoredLight<IBlockExp> light, RuleTest predicate) {
        public boolean matches(BlockState state, net.minecraft.util.RandomSource random) {
            return predicate == AlwaysTrueTest.INSTANCE || predicate.test(state, random);
        }
    }

    private final Map<Block, List<BlockRule>> blocks = new IdentityHashMap<>();
    private final Map<EntityType<?>, ColoredLight<IEntityExp>> entities = new IdentityHashMap<>();
    private final Map<Item, ColoredLight<IEntityExp>> items = new IdentityHashMap<>();
    private final Map<ParticleType<?>, ColoredLight<IParticleExp>> particles = new IdentityHashMap<>();

    public ColoredLightsManager() {
        super(Spec.of("Colored Light", () -> ColoredLightEntry.CODEC)
                .folders("colored_lights")
                .wikiPage("Colored-Lights"));
    }

    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops,
                                  RegistryAccess access) {
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

    public boolean hasBlockLights() {
        return !blocks.isEmpty();
    }

    public boolean hasEntityLights() {
        return !entities.isEmpty() || !items.isEmpty();
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
        if (blocks.isEmpty() && entities.isEmpty() && items.isEmpty() && particles.isEmpty()) return;
        if (!ColoredLightsTracker.activate()) reset();
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        reset();
        ColoredLightsTracker.reset();
    }

    private void reset() {
        blocks.clear();
        entities.clear();
        items.clear();
        particles.clear();
    }
}

*/
