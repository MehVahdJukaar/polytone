package net.mehvahdjukaar.polytone.mixins.forge;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.polytone.fluid.FluidPropertiesManager;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {

    @ModifyVariable(method = "tesselate",
            at = @At(value = "STORE"), ordinal = 0)
    public int polytone$modifyTint(int original, BlockAndTintGetter level, BlockPos pos, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState) {
        return FluidPropertiesManager.modifyColor(original, level, pos, blockState, fluidState);
    }
}
