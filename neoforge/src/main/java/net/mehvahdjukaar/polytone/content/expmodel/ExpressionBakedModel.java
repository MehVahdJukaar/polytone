package net.mehvahdjukaar.polytone.content.expmodel;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// block pos only reaches a model through getModelData, so the case is picked there
public class ExpressionBakedModel implements IDynamicBakedModel {

    private static final ModelProperty<BakedModel> SELECTED = new ModelProperty<>();

    private final ExpressionModel.Selector selector;

    public ExpressionBakedModel(ExpressionModel.Selector selector) {
        this.selector = selector;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        return modelData.derive().with(SELECTED, selector.select(pos, state)).build();
    }

    private BakedModel selected(ModelData data) {
        BakedModel model = data.get(SELECTED);
        return model == null ? selector.fallback() : model;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData data, @Nullable RenderType renderType) {
        return selected(data).getQuads(state, side, rand, data, renderType);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return selected(data).getRenderTypes(state, rand, data);
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
        return selected(data).useAmbientOcclusion(state, data, renderType);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return selected(data).getParticleIcon(data);
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
