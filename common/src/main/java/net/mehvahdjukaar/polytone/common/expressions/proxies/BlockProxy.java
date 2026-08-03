package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

//trying to start declarative here, what is HERE in this block? We won't let users access everything outside of it, thats asking for trouble

//immutable
@BeanAliases
public class BlockProxy extends PositionalProxy {

    // Max distance (per axis, from the source block) a neighbor query may reach. Keeps expressions
    // from walking arbitrarily far into the world.
    public static final int MAX_NEIGHBOR_DEPTH = 8;

    private final Vec3 p;
    private final BlockPos pos;
    private final BlockPos origin;
    private final BlockAndTintGetter level;

    public BlockProxy(@NotNull BlockAndTintGetter level, @Nullable Vec3 pos, @Nullable BlockState state, @Nullable Biome biome) {
        super(state, biome);
        this.p = pos == null ? Vec3.ZERO : pos;
        this.pos = BlockPos.containing(p);
        this.origin = this.pos;
        this.level = level;
    }

    public BlockProxy(@NotNull BlockAndTintGetter level, @Nullable Vec3 pos, @Nullable BlockState state) {
        super(state);
        this.p = pos == null ? Vec3.ZERO : pos;
        this.pos = BlockPos.containing(p);
        this.origin = this.pos;
        this.level = level;
    }

    public BlockProxy(@NotNull BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state) {
        super(state);
        this.p = pos == null ? Vec3.ZERO : Vec3.atBottomCenterOf(pos);
        this.pos = pos;
        this.origin = this.pos;
        this.level = level;
    }

    public BlockProxy(@NotNull BlockAndTintGetter level, Vec3 pos) {
        super();
        this.p = pos;
        this.pos = BlockPos.containing(p);
        this.origin = this.pos;
        this.level = level;
    }

    // neighbor instance: shares the source's origin so the depth cap is measured from the source, not per hop
    private BlockProxy(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockPos origin) {
        super();
        this.p = Vec3.atBottomCenterOf(pos);
        this.pos = pos;
        this.origin = origin;
        this.level = level;
    }

    @Override
    protected BlockAndTintGetter getLevelInternal() {
        return level;
    }

    @Override
    protected BlockPos getPosInternal() {
        return pos;
    }

    public double x() {
        return pos.getX();
    }

    public double y() {
        return pos.getY();
    }

    public double z() {
        return pos.getZ();
    }

    // --- Neighbor navigation ---------------------------------------------------------------------
    // Each accessor returns a new immutable BlockProxy at the offset position, so the full query API
    // (block, biome, skyLight, hasFluid, ...) composes on neighbors: e.g. north.up.block .
    // Offsets are clamped so a chain can never reach further than MAX_NEIGHBOR_DEPTH blocks from the
    // source block on any axis.

    public BlockProxy relative(double dx, double dy, double dz) {
        int nx = clampToDepth(pos.getX() + (int) dx, origin.getX());
        int ny = clampToDepth(pos.getY() + (int) dy, origin.getY());
        int nz = clampToDepth(pos.getZ() + (int) dz, origin.getZ());
        if (nx == pos.getX() && ny == pos.getY() && nz == pos.getZ()) return this;
        return new BlockProxy(level, new BlockPos(nx, ny, nz), origin);
    }

    private static int clampToDepth(int value, int originAxis) {
        return Math.max(originAxis - MAX_NEIGHBOR_DEPTH, Math.min(originAxis + MAX_NEIGHBOR_DEPTH, value));
    }

    public BlockProxy north() {
        return relative(0, 0, -1);
    }

    public BlockProxy south() {
        return relative(0, 0, 1);
    }

    public BlockProxy west() {
        return relative(-1, 0, 0);
    }

    public BlockProxy east() {
        return relative(1, 0, 0);
    }

    public BlockProxy up() {
        return relative(0, 1, 0);
    }

    public BlockProxy down() {
        return relative(0, -1, 0);
    }

}
