package net.mehvahdjukaar.polytone.common.codec_ui.example;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaRecord;
import net.minecraft.core.BlockPos;

/**
 * MC game-type editor: edit a {@link BlockPos}.
 *
 * <p>Pattern: <b>wrapper</b>. Vanilla {@code BlockPos.CODEC} is built on
 * {@code Codec.INT_STREAM} and encodes as a 3-element JSON array {@code [x, y, z]}, which the
 * editor cannot render as named fields. We therefore expose a wrapper record with three
 * separately-editable {@code int} fields. The resulting JSON shape ({@code {x,y,z}}) does NOT
 * match the vanilla codec, but the wrapper's codec still ends up producing a real
 * {@link BlockPos} on decode (via the {@code BlockPos::new} constructor reference).
 */
public record BlockPosExample(int x, int y, int z) {

    /** Convenience: convert this wrapper to a real {@link BlockPos}. */
    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

    /** Convenience: read a {@link BlockPos} into a wrapper instance. */
    public static BlockPosExample of(BlockPos pos) {
        return new BlockPosExample(pos.getX(), pos.getY(), pos.getZ());
    }

    public static final SchemaCodec<BlockPosExample> SCHEMA_CODEC = SchemaRecord.create(
            BlockPosExample.class, i -> i.group(
                    i.field("x", Codec.INT, BlockPosExample::x),
                    i.field("y", Codec.INT, BlockPosExample::y),
                    i.field("z", Codec.INT, BlockPosExample::z)
            ).apply(i, BlockPosExample::new));
}
