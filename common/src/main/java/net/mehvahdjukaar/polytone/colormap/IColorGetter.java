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


    default IColorGetter makeConcurrent(){
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
    }

    //wraps around a color resolver. note that usually the block color get color internally calls the color resolver itself which with grass replacement might be us
    record ofColorResolver(BlockColor bc, ColorResolver cr) implements  IColorGetter,ColorResolver{

        @Override
        public int getColor(BlockState state, @Nullable BlockAndTintGetter reader, @Nullable BlockPos pos, int tintIndex) {
            return bc.getColor(state, reader, pos, tintIndex);
        }

        @Override
        public int getColor(ItemStack itemStack, int i) {
            Minecraft mc = Minecraft.getInstance();
            Level world = mc.level;
            if (world == null) return -1;
            BlockPos pos = mc.player.blockPosition();
            BlockState state = world.getBlockState(pos);
            return bc.getColor(state, world, pos, i) | 0xff000000;

        }

        @Override
        public int getColor(Biome biome, double d, double e) {
            return cr.getColor(biome, d, e);
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
    }
}
