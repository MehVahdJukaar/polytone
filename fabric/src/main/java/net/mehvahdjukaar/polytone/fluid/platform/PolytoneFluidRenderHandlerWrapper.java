package net.mehvahdjukaar.polytone.fluid.platform;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public record PolytoneFluidRenderHandlerWrapper(FluidRenderHandler instance,
                                                BlockColor color) implements FluidRenderHandler {
    @Override
    public TextureAtlasSprite[] getFluidSprites(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state) {
        return instance.getFluidSprites(view, pos, state);
    }

    @Override
    public int getFluidColor(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state) {
        if (color != null) {
            return color.getColor(state.createLegacyBlock(), view, pos, -1);
        }
        return instance.getFluidColor(view, pos, state);
    }

    @Override
    public void reloadTextures(TextureAtlas textureAtlas) {
        instance.reloadTextures(textureAtlas);
    }
}
