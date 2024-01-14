package net.mehvahdjukaar.polytone.fabric;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.fluid.FluidPropertiesManager;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public record PolytoneFluidRenderHandlerWrapper(FluidRenderHandler instance) implements FluidRenderHandler {
    @Override
    public TextureAtlasSprite[] getFluidSprites(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state) {
        return instance.getFluidSprites(view, pos, state);
    }

    @Override
    public int getFluidColor(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state) {
        return Polytone.FLUID_PROPERTIES.modifyColor(instance.getFluidColor(view, pos, state),
                view, pos, state.createLegacyBlock(), state);
    }

    @Override
    public void renderFluid(BlockPos pos, BlockAndTintGetter world, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState) {
        instance.renderFluid(pos, world, vertexConsumer, blockState, fluidState);
    }

    @Override
    public void reloadTextures(TextureAtlas textureAtlas) {
        instance.reloadTextures(textureAtlas);
    }
}
