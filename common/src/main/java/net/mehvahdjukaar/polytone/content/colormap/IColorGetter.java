package net.mehvahdjukaar.polytone.content.colormap;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.content.item.BarColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface IColorGetter extends BlockTintSource, BarColor {

    default boolean needsToFillTexture() {
        return false;
    }

    default IColorGetter makeConcurrent() {
        return this;
    }

    int sampleColor(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos,
                    @Nullable Biome biome, @Nullable ItemStack item);

    @Override
    default int color(BlockState state) {
        return sampleColor(null, state, null, null, null);
    }

    @Override
    default int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return sampleColor(level, state, pos.getCenter(), null, null);
    }

    record OfBlock(BlockTintSource bc) implements IColorGetter {

        @Override
        public int color(BlockState state) {
            return bc.color(state);
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter reader, BlockPos pos) {
            return bc.colorInWorld(state, reader, pos);
        }

        @Override
        public int getItemColor(ItemStack itemStack, int i) {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel world = mc.level;
            if (world == null) return -1;
            BlockPos pos = mc.player.blockPosition();
            BlockState state = world.getBlockState(pos);
            return bc.colorInWorld(state, world, pos) | 0xff000000;
        }

        @Override
        public int sampleColor(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable ItemStack item) {
            if (state != null && pos != null) {
                return bc.color(state) | 0xff000000;
            }
            return -1;
        }

    }

    //wraps around a color resolver. note that usually the block color get color internally calls the color resolver itself which with grass replacement might be us
    record OfColorResolver(BlockTintSource bc, ColorResolver cr) implements IColorGetter, ColorResolver {

        @Override
        public int color(BlockState state) {
            return bc.color(state);
        }

        @Override
        public int colorInWorld(BlockState state, @Nullable BlockAndTintGetter reader, BlockPos pos) {
            return bc.colorInWorld(state, reader, pos);
        }

        @Override
        public int getItemColor(ItemStack stack, int tintIndex) {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel world = mc.level;
            if (world == null) return -1;
            BlockPos pos = mc.player.blockPosition();
            BlockState state = world.getBlockState(pos);
            return bc.colorInWorld(state, world, pos) | 0xff000000;
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
        public int color(BlockState state) {
            return ic.getItemColor(ItemStack.EMPTY, 0);
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
        public int color(BlockState state) {
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
            if (pos == null) {
                return 0;
            }
            return (int) exp.evaluate(Minecraft.getInstance().level, pos, state);
        }

        @Override
        public int getItemColor(ItemStack stack, int tintIndex) {
            return (int) exp.evaluate(Minecraft.getInstance().level, Vec3.ZERO, Blocks.AIR.defaultBlockState());
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            ClientLevel cl = level instanceof ClientLevel c ? c : Minecraft.getInstance().level;
            return (int) exp.evaluate(cl, pos.getCenter(), state);
        }

    }


    record OfLayers(List<BlockTintSource> layers) implements IColorGetter {
        @Override
        public int color(BlockState state) {
            return layers.isEmpty() ? -1 : layers.get(0).color(state);
        }

        @Override
        public int getItemColor(ItemStack stack, int tintIndex) {
            return tintIndex < layers.size() ? layers.get(tintIndex).color(Blocks.AIR.defaultBlockState()) : -1;
        }

        @Override
        public int sampleColor(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable ItemStack item) {
            if (state == null) return -1;
            return layers.isEmpty() ? -1 : layers.get(0).color(state);
        }
    }

    SchemaCodec<IColorGetter> SINGLE_COLOR_CODEC = SchemaCodecs.xmap(ColorUtils.COLOR,
            IColorGetter.StaticColor::new, g -> g instanceof StaticColor(int color) ? color : 0
    );

    Codec<IColorGetter> EXPRESSION_CODEC = IBlockExp.CODEC.xmap(
            IColorGetter.ExpressionColor::new,
            g -> g instanceof ExpressionColor(IBlockExp exp) ? exp : IBlockExp.ZERO
    );

    // One labeled picker: a color, or an MVEL expression.
    SchemaCodec<IColorGetter> SINGLE_COLOR_OR_EXPRESSION = SchemaCodecs.withAlternative(
            SchemaCodecs.alt("color", SINGLE_COLOR_CODEC),
            SchemaCodecs.alt("expression", EXPRESSION_CODEC));
}
