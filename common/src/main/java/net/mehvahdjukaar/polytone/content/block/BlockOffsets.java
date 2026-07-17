package net.mehvahdjukaar.polytone.content.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
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
                    .offsetFunction).orElse((a, c, b) -> Vec3.ZERO));
        }

        @Override
        public Vec3 evaluate(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
            return inner.evaluate(blockState,blockGetter, blockPos);
        }
    }


    private record CustomOffset(float maxX, float maxY, float maxZ) implements BlockBehaviour.OffsetFunction {
        public static final Codec<CustomOffset> CODEC = RecordCodecBuilder.create(instance -> instance.group(

                Codec.FLOAT.fieldOf("max_x").orElse(0.25f).forGetter(CustomOffset::maxX),
                Codec.FLOAT.fieldOf("max_y").orElse(0.2f).forGetter(CustomOffset::maxY),
                Codec.FLOAT.fieldOf("max_z").orElse(0.25f).forGetter(CustomOffset::maxZ)
        ).apply(instance, CustomOffset::new));

        @Override
        public Vec3 evaluate(BlockState blockState,BlockGetter blockGetter , BlockPos blockPos) {
            long seed = Mth.getSeed(blockPos.getX(), 0, blockPos.getZ());
            double verticalOff = ((double) ((float) (seed >> 4 & 15L) / 15.0F) - 1.0) * (double) maxY;
            double xOff = Mth.clamp(((double) ((float) (seed & 15L) / 15.0F) - 0.5) * 0.5,  (-maxX),  maxX);
            double zOff = Mth.clamp(((double) ((float) (seed >> 8 & 15L) / 15.0F) - 0.5) * 0.5,  (-maxZ),  maxZ);
            return new Vec3(xOff, verticalOff, zOff);

        }
    }


}