package net.mehvahdjukaar.polytone.content.light;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.compat.VeilCompat;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.IEntityExp;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.IParticleExp;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.util.Mth;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ColoredLightsTracker {

    private static final int MAX_BLOCK_LIGHTS = 512;
    private static final int MAX_ENTITY_LIGHTS = 256;
    private static final float DEFAULT_LIGHT_RADIUS = 8;

    private static final Map<Long, List<LitBlock>> litBlocksPerSection = new ConcurrentHashMap<>();
    private static final LongOpenHashSet litBlocks = new LongOpenHashSet();
    private static final LongOpenHashSet litEntities = new LongOpenHashSet();
    private static List<LitEntity> activeEntities = List.of();
    private static final List<LitParticle> activeParticles = new ArrayList<>();
    private static long nextParticleKey;

    private static boolean active;

    public record LitBlock(long pos, BlockState state, ColoredLightsManager.BlockRule rule) {
    }

    static boolean activate() {
        if (!CompatHandler.VEIL) {
            Polytone.LOGGER.info("Resource packs define colored lights but Veil is not installed. Install it to see them");
            return false;
        }
        active = Polytone.CONFIGS.coloredLights.get();
        return active;
    }

    static void reset() {
        litBlocksPerSection.clear();
        litBlocks.clear();
        litEntities.clear();
        activeEntities = List.of();
        activeParticles.clear();
        if (active) VeilCompat.clearAll();
    }

    public static final class Scan {
        private static final int BLOCKS_PER_SECTION = 16 * 16 * 16;

        private final List<LitBlock> found = new ArrayList<>();
        private final RandomSource random = RandomSource.create();
        private long sectionKey = Long.MIN_VALUE;
        private int seen;

        public void offer(int x, int y, int z, BlockState state) {
            if (sectionKey == Long.MIN_VALUE) {
                sectionKey = SectionPos.asLong(SectionPos.blockToSectionCoord(x),
                        SectionPos.blockToSectionCoord(y), SectionPos.blockToSectionCoord(z));
            }
            seen++;
            var rules = Polytone.COLORED_LIGHTS.getBlockLights(state.getBlock());
            if (rules == null) return;
            for (var rule : rules) {
                if (rule.matches(state, random)) {
                    found.add(new LitBlock(BlockPos.asLong(x, y, z), state, rule));
                    return;
                }
            }
        }
    }

    @Nullable
    public static Scan openScan() {
        return active && Polytone.COLORED_LIGHTS.hasBlockLights() ? new Scan() : null;
    }

    public static void publishSection(@Nullable Scan scan) {
        if (scan == null || scan.seen < Scan.BLOCKS_PER_SECTION) return;
        if (scan.found.isEmpty()) litBlocksPerSection.remove(scan.sectionKey);
        else litBlocksPerSection.put(scan.sectionKey, List.copyOf(scan.found));
    }

    public static void onTick(ClientLevel level, BlockPos camera) {
        if (!active) return;
        pushBlocks(level, camera.getCenter());
        pushEntities(level, camera.getCenter());
        pushParticles(level);
    }

    public static void onFrame(float partialTicks) {
        if (!active) return;
        for (LitEntity lit : activeEntities) {
            Entity e = lit.entity;
            Vec3 pos = e.getPosition(partialTicks);
            VeilCompat.moveEntityLight(e.getId(), pos.x, pos.y + e.getBbHeight() * 0.5, pos.z);
        }
        for (LitParticle lit : activeParticles) {
            Particle p = lit.particle;
            VeilCompat.moveParticleLight(lit.key,
                    Mth.lerp(partialTicks, p.xo, p.x),
                    Mth.lerp(partialTicks, p.yo, p.y),
                    Mth.lerp(partialTicks, p.zo, p.z));
        }
    }

    public static void onParticleCreated(ParticleType<?> type, Particle particle) {
        if (!active) return;
        var light = Polytone.COLORED_LIGHTS.getParticleLight(type);
        if (light != null) activeParticles.add(new LitParticle(particle, light, nextParticleKey++));
    }

    private static void pushParticles(ClientLevel level) {
        var it = activeParticles.iterator();
        while (it.hasNext()) {
            LitParticle lit = it.next();
            if (!lit.particle.isAlive()) {
                VeilCompat.removeParticleLight(lit.key);
                it.remove();
                continue;
            }
            Particle p = lit.particle;
            LightProperties props = lit.light.resolve(exp -> exp.evaluate(p, level), DEFAULT_LIGHT_RADIUS);
            VeilCompat.setParticleLight(lit.key, p.x, p.y, p.z, props);
        }
    }

    private record LitParticle(Particle particle, ColoredLight<IParticleExp> light, long key) {
    }

    private static void pushBlocks(ClientLevel level, Vec3 camera) {
        if (!Polytone.COLORED_LIGHTS.hasBlockLights()) {
            if (!litBlocksPerSection.isEmpty() || !litBlocks.isEmpty()) {
                litBlocksPerSection.clear();
                litBlocks.clear();
                VeilCompat.clearBlockLights();
            }
            return;
        }

        List<LitBlock> found = new ArrayList<>();
        var it = litBlocksPerSection.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            long key = e.getKey();
            if (!level.getChunkSource().hasChunk(SectionPos.x(key), SectionPos.z(key))) {
                it.remove();
                continue;
            }
            found.addAll(e.getValue());
        }
        if (found.size() > MAX_BLOCK_LIGHTS) {
            found.sort(Comparator.comparingDouble(b -> BlockPos.of(b.pos).getCenter().distanceToSqr(camera)));
            found.subList(MAX_BLOCK_LIGHTS, found.size()).clear();
        }

        LongOpenHashSet stale = new LongOpenHashSet(litBlocks);
        for (LitBlock lit : found) {
            Vec3 center = BlockPos.of(lit.pos).getCenter();
            LightProperties props = lit.rule.light().resolve(
                    exp -> exp.evaluate(level, center, lit.state), defaultRadius(lit.state));
            VeilCompat.setBlockLight(lit.pos, center.x, center.y, center.z, props);
            litBlocks.add(lit.pos);
            stale.remove(lit.pos);
        }
        stale.forEach(VeilCompat::removeBlockLight);
        litBlocks.removeAll(stale);
    }

    private static float defaultRadius(BlockState state) {
        int emission = state.getLightEmission();
        return emission > 0 ? emission : DEFAULT_LIGHT_RADIUS;
    }

    private static void pushEntities(ClientLevel level, Vec3 camera) {
        if (!Polytone.COLORED_LIGHTS.hasEntityLights()) {
            if (!litEntities.isEmpty()) {
                litEntities.clear();
                activeEntities = List.of();
                VeilCompat.clearEntityLights();
            }
            return;
        }

        List<LitEntity> found = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            var light = lightFor(entity);
            if (light != null) found.add(new LitEntity(entity, light));
        }
        if (found.size() > MAX_ENTITY_LIGHTS) {
            found.sort(Comparator.comparingDouble(e -> e.entity.distanceToSqr(camera)));
            found.subList(MAX_ENTITY_LIGHTS, found.size()).clear();
        }

        LongOpenHashSet stale = new LongOpenHashSet(litEntities);
        for (LitEntity lit : found) {
            Entity entity = lit.entity;
            long key = entity.getId();
            Vec3 pos = entity.position();
            LightProperties props = lit.light.resolve(exp -> exp.evaluate(entity), DEFAULT_LIGHT_RADIUS);
            VeilCompat.setEntityLight(key, pos.x, pos.y + entity.getBbHeight() * 0.5, pos.z, props);
            litEntities.add(key);
            stale.remove(key);
        }
        stale.forEach(VeilCompat::removeEntityLight);
        litEntities.removeAll(stale);
        activeEntities = found;
    }

    private record LitEntity(Entity entity, ColoredLight<IEntityExp> light) {
    }

    @Nullable
    private static ColoredLight<IEntityExp> lightFor(Entity entity) {
        if (entity instanceof ItemEntity item) {
            var light = Polytone.COLORED_LIGHTS.getItemLight(item.getItem().getItem());
            if (light != null) return light;
        }
        var light = Polytone.COLORED_LIGHTS.getEntityLight(entity.getType());
        if (light != null) return light;
        if (entity instanceof LivingEntity living) {
            for (InteractionHand hand : InteractionHand.values()) {
                var held = Polytone.COLORED_LIGHTS.getItemLight(living.getItemInHand(hand).getItem());
                if (held != null) return held;
            }
        }
        return null;
    }
}
