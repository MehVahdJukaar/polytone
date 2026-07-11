package net.mehvahdjukaar.polytone.content.block;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Optional;

public class BlockOffsets {

    public static final Codec<BlockBehaviour.OffsetFunction> CODEC = SchemaCodecs.alternatives(
            "type", TypeOffset.CODEC,
            "custom", CustomOffset.CODEC);

    private record TypeOffset(BlockBehaviour.OffsetType type,
                              BlockBehaviour.OffsetFunction inner) implements BlockBehaviour.OffsetFunction {

        public static final Codec<TypeOffset> CODEC = Codec.STRING
                .xmap(s -> {
                    try {
                        return BlockBehaviour.OffsetType.valueOf(s.toUpperCase(Locale.ROOT));
                    } catch (Exception e) {
                        return BlockBehaviour.OffsetType.NONE;
                    }
                }, i -> i.name().toLowerCase(Locale.ROOT))
                .xmap(TypeOffset::new, TypeOffset::type);

        private TypeOffset(BlockBehaviour.OffsetType type) {
            this(type, Optional.ofNullable(BlockBehaviour.Properties.of().offsetType(type)
                    .offsetFunction).orElse((a, b) -> Vec3.ZERO));
        }

        @Override
        public Vec3 evaluate(BlockState blockState, BlockPos blockPos) {
            return inner.evaluate(blockState, blockPos);
        }
    }


    private record CustomOffset(float maxX, float maxY, float maxZ) implements BlockBehaviour.OffsetFunction {
        public static final SchemaCodec<CustomOffset> CODEC = SchemaRecord.create(CustomOffset.class, i -> i.group(

                i.optional("max_x", Codec.FLOAT, 0.25f, CustomOffset::maxX),
                i.optional("max_y", Codec.FLOAT, 0.2f, CustomOffset::maxY),
                i.optional("max_z", Codec.FLOAT, 0.25f, CustomOffset::maxZ)
        ).apply(i, CustomOffset::new));

        @Override
        public Vec3 evaluate(BlockState blockState , BlockPos blockPos) {
            //same stuff that blocks do
            long seed = Mth.getSeed(blockPos.getX(), 0, blockPos.getZ());
            double verticalOff = ((double) ((float) (seed >> 4 & 15L) / 15.0F) - 1.0) * (double) maxY;
            double xOff = Mth.clamp(((double) ((float) (seed & 15L) / 15.0F) - 0.5) * 0.5,  (-maxX),  maxX);
            double zOff = Mth.clamp(((double) ((float) (seed >> 8 & 15L) / 15.0F) - 0.5) * 0.5,  (-maxZ),  maxZ);
            return new Vec3(xOff, verticalOff, zOff);

        }
    }


}