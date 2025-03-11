package net.mehvahdjukaar.polytone.fluid.fabric;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public record PolytoneFluidRenderHandlerWrapper(FluidRenderHandler instance,
                                                BlockColor color) implements FluidRenderHandler {

    static final boolean SODIUM_ON = FabricLoader.getInstance().isModLoaded("sodium");

    @Override
    public TextureAtlasSprite[] getFluidSprites(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state) {
        return instance.getFluidSprites(view, pos, state);
    }

    @Override
    public int getFluidColor(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state) {
        int c = color.getColor(state.createLegacyBlock(), view, pos, -1);
        if (SODIUM_ON) {
            var p = ColorUtils.unpack(c);
            return ColorUtils.pack(p);
            //return ColorUtils.pack(p[2], p[1], p[0]);
        }
        return c;

        //  return instance.getFluidColor(view, pos, state);
    }

    @Override
    public void reloadTextures(TextureAtlas textureAtlas) {
        instance.reloadTextures(textureAtlas);
    }
}
