package net.mehvahdjukaar.polytone.content.expmodel;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.mehvahdjukaar.polytone.common.exp.impl.BlockContextExpression;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Fabric baked form of the {@code polytone:expression} model. See {@link ExpressionModel} for the
 * shared selection logic and JSON shape. Fabric feeds block context through {@code emitQuads}, so
 * we select there and delegate to the chosen sub-model's own emission (keeping its weighted random).
 */
public record ExpressionBlockStateModel(ExpressionModel.Selector selector) implements BlockStateModel {

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cullTest) {
        selector.select(pos, state).emitQuads(emitter, blockView, pos, state, random, cullTest);
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random) {
        // Fold the resolved case into the mesh-merge key so neighbours that pick different
        // models are never greedily merged or wrongly culled against each other.
        return selector.selectIndex(pos, state);
    }

    @Override
    public TextureAtlasSprite particleSprite(BlockAndTintGetter blockView, BlockPos pos, BlockState state) {
        return selector.select(pos, state).particleSprite(blockView, pos, state);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts) {
        // no world context (e.g. inventory/baking probes) -> deterministic fallback
        selector.fallback().collectParts(random, parts);
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return selector.particleIcon();
    }

    public record Unbaked(List<ExpressionModel.Case> cases, Optional<BlockContextExpression> selector,
                          BlockStateModel.Unbaked fallback) implements CustomUnbakedBlockStateModel {

        public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ExpressionModel.Case.CODEC.listOf().fieldOf("cases").forGetter(Unbaked::cases),
                BlockContextExpression.CODEC.optionalFieldOf("selector").forGetter(Unbaked::selector),
                BlockStateModel.Unbaked.CODEC.fieldOf("fallback").forGetter(Unbaked::fallback)
        ).apply(i, Unbaked::new));

        @Override
        public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
            return CODEC;
        }

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            return new ExpressionBlockStateModel(ExpressionModel.bake(cases, selector, fallback, baker));
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            ExpressionModel.resolveDependencies(cases, fallback, resolver);
        }
    }
}
