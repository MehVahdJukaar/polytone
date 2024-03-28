package net.mehvahdjukaar.polytone.fluid.fabric;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public record PolytoneFluidRenderHandlerWrapper(FluidRenderHandler instance,
                                                FluidPropertyModifier modifier) implements FluidRenderHandler {

    static final boolean SODIUM_ON = FabricLoader.getInstance().isModLoaded("sodium");

    @Override
    public TextureAtlasSprite[] getFluidSprites(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state) {
        return instance.getFluidSprites(view, pos, state);
    }

    @Override
    public int getFluidColor(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state) {
        var col = modifier.getColormap();
        if (col != null) {
            int color = col.getColor(state.createLegacyBlock(), view, pos, -1);
            if (SODIUM_ON) {
                var p = ColorUtils.unpack(color);
                return ColorUtils.pack(p[2], p[1], p[0]);
            }
            return color;

        }
        return instance.getFluidColor(view, pos, state);
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
