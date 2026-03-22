package net.mehvahdjukaar.polytone.content.colormap;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.content.item.BarColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface IColorGetter extends BlockColor, BarColor {

    default boolean needsToFillTexture() {
        return false;
    }

    default IColorGetter makeConcurrent() {
        return this;
    }

    int sampleColor(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos,
                    @Nullable Biome biome, @Nullable ItemStack item);

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
        public int sampleColor(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable ItemStack item) {
            if (state != null && pos != null) {
                return bc.getColor(state, null, BlockPos.containing(pos), 0) | 0xff000000;
            }
            return -1;
        }

    }

    //wraps around a color resolver. note that usually the block color get color internally calls the color resolver itself which with grass replacement might be us
    record OfColorResolver(BlockColor bc, ColorResolver cr) implements IColorGetter, ColorResolver {


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
        public int sampleColor(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable ItemStack item) {
            if (biome != null) {
                int x = pos == null ? 0 : (int) pos.x();
                int z = pos == null ? 0 : (int) pos.z();
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
        public int sampleColor(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable ItemStack item) {
            return ic.getItemColor(item == null ? ItemStack.EMPTY : item, 0);
        }

    }

    record StaticColor(int color) implements IColorGetter {

        @Override
        public int getColor(BlockState state, BlockAndTintGetter reader, BlockPos pos, int tintIndex) {
            return color;
        }

        @Override
        public int getItemColor(ItemStack itemStack, int i) {
            return color;
        }

        @Override
        public int sampleColor(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable ItemStack item) {
            return color;
        }

    }

    //TODO: proper exp here
    record ExpressionColor(IBlockExp exp) implements IColorGetter {
        @Override
        public int sampleColor(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable ItemStack item) {
            if (pos == null || state == null) {
                return 0;
            }
            return (int) exp.evaluate(Minecraft.getInstance().level, pos, state);
        }

        @Override
        public int getItemColor(ItemStack stack, int tintIndex) {
            return (int) exp.evaluate(Minecraft.getInstance().level, Vec3.ZERO, Blocks.AIR.defaultBlockState());
        }

        @Override
        public int getColor(BlockState blockState, @Nullable BlockAndTintGetter blockAndTintGetter, @Nullable BlockPos blockPos, int i) {
            if (blockAndTintGetter instanceof LevelReader lr && blockPos != null) {
                return (int) exp.evaluate(lr, blockPos.getCenter(), blockState);
            }
            return 0;
        }

    }


    Codec<IColorGetter> SINGLE_COLOR_CODEC = ColorUtils.COLOR.xmap(
            IColorGetter.StaticColor::new, g -> g instanceof StaticColor(int color) ? color : 0
    );

    Codec<IColorGetter> EXPRESSION_CODEC = IBlockExp.CODEC.xmap(
            IColorGetter.ExpressionColor::new,
            g -> g instanceof ExpressionColor(IBlockExp exp) ? exp : IBlockExp.ZERO
    );

    Codec<IColorGetter> SINGLE_COLOR_OR_EXPRESSION = Codec.withAlternative(
            SINGLE_COLOR_CODEC, EXPRESSION_CODEC);
}
