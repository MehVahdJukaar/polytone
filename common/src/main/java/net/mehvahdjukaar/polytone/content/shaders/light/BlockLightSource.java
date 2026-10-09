package net.mehvahdjukaar.polytone.content.shaders.light;

import net.mehvahdjukaar.polytone.api.ResolvedPointLight;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//Only used by veil
public class BlockLightSource extends LightSource {

    private static final int MAX_LIGHTS = 512;

    private final Map<Long, List<LitBlockRecord>> litBlocksPerSection = new ConcurrentHashMap<>();

    public BlockLightSource(PointLightStorage storage) {
        super(storage);
    }

    public Scan openScan() {
        return new Scan();
    }

    public class Scan {

        private static final int BLOCKS_PER_SECTION = 16 * 16 * 16;
        private final List<LitBlockRecord> found = new ArrayList<>();

        private final RandomSource random = RandomSource.create();
        private long sectionKey = Long.MIN_VALUE;
        private int seen;

        public void offer(int x, int y, int z, BlockState state) {
            if (sectionKey == Long.MIN_VALUE) {
                sectionKey = SectionPos.asLong(SectionPos.blockToSectionCoord(x),
                        SectionPos.blockToSectionCoord(y), SectionPos.blockToSectionCoord(z));
            }
            seen++;
            BlockState lightState = Polytone.COLORED_LIGHTS.lightStateOf(state);
            var rules = Polytone.COLORED_LIGHTS.getBlockLights(lightState.getBlock());
            if (rules == null) return;
            for (var rule : rules) {
                if (rule.matches(lightState, random)) {
                    found.add(new LitBlockRecord(new BlockPos(x, y, z), lightState, rule));
                    return;
                }
            }
        }

        public void publish() {
            if (seen < BLOCKS_PER_SECTION) return;
            if (found.isEmpty()) litBlocksPerSection.remove(sectionKey);
            else litBlocksPerSection.put(sectionKey, List.copyOf(found));
        }
    }

    @Override
    public void tick(ClientLevel level, Vec3 camera) {
        List<LitBlockRecord> found = new ArrayList<>();
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
        keepNearest(found, MAX_LIGHTS, b -> b.pos.getCenter().distanceToSqr(camera));

        for (LitBlockRecord lit : found) {
            Vec3 center = lit.pos.getCenter();
            ResolvedPointLight resolved = lit.rule.light().resolve(lit.state, center, level);
            if (resolved != null) {
                set(lit.pos, center.x, center.y, center.z, resolved);
            }
        }
        removeUnset();
    }

    @Override
    public void clear() {
        super.clear();
        litBlocksPerSection.clear();
    }

    private record LitBlockRecord(BlockPos pos, BlockState state, ColoredLightsManager.BlockRule rule) {
        ResolvedPointLight resolve(ClientLevel level) {
            return rule.light().resolve(state, pos.getCenter(), level);
        }
    }
}
