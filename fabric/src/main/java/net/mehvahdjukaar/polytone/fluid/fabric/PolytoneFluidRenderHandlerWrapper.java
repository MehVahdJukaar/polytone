package net.mehvahdjukaar.polytone.fluid.fabric;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public record PolytoneFluidRenderHandlerWrapper(FluidRenderHandler instance,
                                                FluidPropertyModifier modifier) implements FluidRenderHandler {
    @Override
    public TextureAtlasSprite[] getFluidSprites(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state) {
         return instance.getFluidSprites(view, pos, state);
    }

    @Override
    public int getFluidColor(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state) {
        BlockColor col = modifier.getColormap();
        if (col != null) {
            return col.getColor(state.createLegacyBlock(), view, pos, -1);
        }
        return instance.getFluidColor(view, pos, state);
    }

    @Override
    public void reloadTextures(TextureAtlas textureAtlas) {
        instance.reloadTextures(textureAtlas);
    }
}
