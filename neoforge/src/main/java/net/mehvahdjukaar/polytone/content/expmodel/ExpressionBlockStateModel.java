package net.mehvahdjukaar.polytone.content.expmodel;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.expressions.impl.BlockExp;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

import java.util.List;
import java.util.Optional;

// NeoForge baked form of the polytone:expression model
public record ExpressionBlockStateModel(ExpressionModel.Selector selector) implements DynamicBlockStateModel {

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        selector.select(pos, state).collectParts(level, pos, state, random, parts);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        selector.fallback().collectParts(random, parts);
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return selector.select(pos, state).createGeometryKey(level, pos, state, random);
    }

    @Override
    public Material.Baked particleMaterial() {
        return selector.particleMaterial();
    }

    @Override
    public int materialFlags() {
        return selector.materialFlags();
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return selector.select(pos, state).particleMaterial(level, pos, state);
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return selector.select(pos, state).materialFlags(level, pos, state);
    }

    public record Unbaked(List<ExpressionModel.Case> cases, Optional<BlockExp> selector,
                          BlockStateModel.Unbaked fallback) implements CustomUnbakedBlockStateModel {

        public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ExpressionModel.Case.CODEC.listOf().fieldOf("cases").forGetter(Unbaked::cases),
                BlockExp.TYPE.codec().optionalFieldOf("selector").forGetter(Unbaked::selector),
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
