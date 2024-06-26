package net.mehvahdjukaar.polytone.colormap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface IColorGetter extends BlockColor, ItemColor {


    record OfBlock(BlockColor bc) implements IColorGetter {
        @Override
        public int getColor(BlockState state, BlockAndTintGetter reader, BlockPos pos, int tintIndex) {
            return bc.getColor(state, reader, pos, tintIndex);
        }

        @Override
        public int getColor(ItemStack itemStack, int i) {
            Minecraft mc = Minecraft.getInstance();
            Level world = mc.level;
            if (world == null) return -1;
            BlockPos pos = mc.player.blockPosition();
            BlockState state = world.getBlockState(pos);
            return bc.getColor(state, world, pos, i);
        }
    }

    record OfItem(ItemColor ic) implements IColorGetter {
        @Override
        public int getColor(BlockState state, BlockAndTintGetter reader, BlockPos pos, int tintIndex) {
            return ic.getColor(ItemStack.EMPTY, tintIndex);
        }

        @Override
        public int getColor(ItemStack itemStack, int i) {
            return ic.getColor(itemStack, i);
        }
    }
}
