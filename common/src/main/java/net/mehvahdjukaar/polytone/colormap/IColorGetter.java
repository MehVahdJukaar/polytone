package net.mehvahdjukaar.polytone.colormap;

import net.mehvahdjukaar.polytone.item.BarColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface IColorGetter extends BlockColor, BarColor {

    default boolean needsToFillTexture() {
        return false;
    }

    default IColorGetter makeConcurrent() {
        return this;
    }

    record OfBlock(BlockColor bc) implements IColorGetter {
        @Override
        public int getColor(BlockState state, BlockAndTintGetter reader, BlockPos pos, int tintIndex) {
            return bc.getColor(state, reader, pos, tintIndex);
        }

        @Override
        public int getItemColor(ItemStack itemStack, int i) {
            Minecraft mc = Minecraft.getInstance();
            Level world = mc.level;
            if (world == null) return -1;
            BlockPos pos = mc.player.blockPosition();
            BlockState state = world.getBlockState(pos);
            return bc.getColor(state, world, pos, i) | 0xff000000;
        }

        @Override
        public int sampleColor(@Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome, @Nullable ItemStack item) {
            if (state != null && pos != null) {
                return bc.getColor(state, null, pos, 0) | 0xff000000;
            }
            return -1;
        }
    }

    //wraps around a color resolver. note that usually the block color get color internally calls the color resolver itself which with grass replacement might be us
    record ofColorResolver(BlockColor bc, ColorResolver cr) implements IColorGetter, ColorResolver {

        @Override
        public int getColor(BlockState state, @Nullable BlockAndTintGetter reader, @Nullable BlockPos pos, int tintIndex) {
            return bc.getColor(state, reader, pos, tintIndex);
        }

        @Override
        public int getItemColor(ItemStack stack, int tintIndex) {
            Minecraft mc = Minecraft.getInstance();
            Level world = mc.level;
            if (world == null) return -1;
            BlockPos pos = mc.player.blockPosition();
            BlockState state = world.getBlockState(pos);
            return bc.getColor(state, world, pos, tintIndex) | 0xff000000;

        }

        @Override
        public int getColor(Biome biome, double x, double z) {
            return cr.getColor(biome, x, z);
        }

        @Override
        public int sampleColor(@Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome, @Nullable ItemStack item) {
            if (biome != null) {
                int x = pos == null ? 0 : pos.getX();
                int z = pos == null ? 0 : pos.getZ();
                return cr.getColor(biome, x, z);
            }
            return -1;
        }
    }

    record OfItem(BarColor ic) implements IColorGetter {
        @Override
        public int getColor(BlockState state, BlockAndTintGetter reader, BlockPos pos, int tintIndex) {
            return ic.getItemColor(ItemStack.EMPTY, tintIndex);
        }

        @Override
        public int getItemColor(ItemStack itemStack, int i) {
            return ic.getItemColor(itemStack, i);
        }

        @Override
        public int sampleColor(@Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome, @Nullable ItemStack item) {
            return ic.getItemColor(item == null ? ItemStack.EMPTY : item, 0);
        }
    }


    int sampleColor(@Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome, @Nullable ItemStack item);
}
