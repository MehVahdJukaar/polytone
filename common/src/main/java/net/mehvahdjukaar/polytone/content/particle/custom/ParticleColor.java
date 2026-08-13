package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

// Pure config, shared by every particle of a type; the per-particle refresh state lives in Cache.
// Decodes two ways so old packs keep working: a bare colormap (historical, no caching) or a
// compound {colormap, cache} naming the policy.
public record ParticleColor(IColorGetter getter, CachePolicy policy) {

    private static final SchemaCodec<ParticleColor> WITH_OPTIONS = SchemaRecord.create(ParticleColor.class, i -> i.group(
            i.field("colormap", Colormap.CODEC, ParticleColor::getter),
            i.optional("cache", CachePolicy.CODEC, CachePolicy.NONE, ParticleColor::policy)
    ).apply(i, ParticleColor::new));

    // backward compat: a bare colormap (reference / inline / color / expression / biome compound)
    // decodes to the compound form with the default (re-sample every tick) policy.
    public static final SchemaCodec<ParticleColor> CODEC = SchemaCodecs.withAlternative(
            SchemaCodecs.alt("with options", WITH_OPTIONS),
            SchemaCodecs.alt("colormap", SchemaCodecs.xmap(Colormap.CODEC,
                    g -> new ParticleColor(g, CachePolicy.NONE),
                    ParticleColor::getter)));

    public enum CachePolicy implements StringRepresentable {
        ON_SPAWN,
        PER_POSITION,
        NONE;

        public static final Codec<CachePolicy> CODEC = StringRepresentable.fromEnum(CachePolicy::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    // re-samples the colour onto the particle per the cache policy, only when stale; mirrors the
    // block/section-version invalidation of ParticleLightCache.Entry
    public static final class Cache {
        private final CustomParticleInstance p;
        private final ParticleColor colormap;
        private long blockKey = Long.MIN_VALUE; // forces the first sample
        private int sectionVersion;

        Cache(CustomParticleInstance p, ParticleColor colormap, @Nullable BlockState spawnState, BlockPos spawnPos) {
            this.colormap = colormap;
            this.p = p;
            sampleAndApply(p.getLevel(), spawnState, spawnPos);
        }

        void tick() {
            switch (colormap.policy()) {
                case ON_SPAWN -> { /* frozen at spawn */ }
                case NONE -> resample();
                case PER_POSITION -> {
                    int bx = Mth.floor(p.x), by = Mth.floor(p.y), bz = Mth.floor(p.z);
                    long key = BlockPos.asLong(bx, by, bz);
                    int version = ParticleLightCache.sectionVersion(bx >> 4, by >> 4, bz >> 4);
                    if (key != blockKey || version != sectionVersion) resample();
                }
            }
        }

        private void resample() {
            Level level = p.getLevel();
            BlockPos pos = BlockPos.containing(p.x, p.y, p.z);
            // off-thread safe: skip unloaded chunks so we don't sample an air fallback
            if (!level.hasChunkAt(pos)) return;
            sampleAndApply(level,level.getBlockState(pos), pos);
        }

        private void sampleAndApply(Level level, @Nullable BlockState state, BlockPos pos) {
            blockKey = pos.asLong();
            sectionVersion = ParticleLightCache.sectionVersion(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            float[] unpack = ColorUtils.unpack(colormap.getter().getColor(state, level, pos, 0));
            p.rCol = unpack[0];
            p.gCol = unpack[1];
            p.bCol = unpack[2];
        }
    }

}
