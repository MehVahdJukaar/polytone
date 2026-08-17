package net.mehvahdjukaar.polytone.content.expmodel;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

// FRAPI is the only path that gets a block pos, so the case is picked in emitBlockQuads
public class ExpressionBakedModel implements BakedModel, FabricBakedModel {

    private final ExpressionModel.Selector selector;

    public ExpressionBakedModel(ExpressionModel.Selector selector) {
        this.selector = selector;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos,
                               Supplier<RandomSource> randomSupplier, RenderContext context) {
        ((FabricBakedModel) selector.select(pos, state)).emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
        ((FabricBakedModel) selector.fallback()).emitItemQuads(stack, randomSupplier, context);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
        return selector.fallback().getQuads(state, direction, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return selector.fallback().useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return selector.fallback().isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return selector.fallback().usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return selector.fallback().getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return selector.fallback().getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return selector.fallback().getOverrides();
    }
}
