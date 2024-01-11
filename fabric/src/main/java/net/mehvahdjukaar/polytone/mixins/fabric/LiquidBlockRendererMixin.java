package net.mehvahdjukaar.polytone.mixins.fabric;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.impl.client.rendering.fluid.FluidRendererHookContainer;
import net.mehvahdjukaar.polytone.fluid.FluidPropertiesManager;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = LiquidBlockRenderer.class, priority = 1200)
public class LiquidBlockRendererMixin {


    //same as fabric but higher priority so we wrap it
    @ModifyVariable(at = @At(value = "CONSTANT", args = "intValue=16", ordinal = 0, shift = At.Shift.BEFORE),
            method = "tesselate", ordinal = 0)
    public int polytone$modifyTint(int original, BlockAndTintGetter level, BlockPos pos, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState) {
        return FluidPropertiesManager.modifyColor(original, level, pos, blockState, fluidState);
    }
}
