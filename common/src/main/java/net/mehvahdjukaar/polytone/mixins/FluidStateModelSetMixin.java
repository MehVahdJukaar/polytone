package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FluidStateModelSet.class)
public abstract class FluidStateModelSetMixin {

    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    private FluidModel polytone$applyFluidColormap(FluidModel original, @Local(argsOnly = true) FluidState state) {
        return Polytone.FLUID_MODIFIERS.getTintedModel(state.getType(), original);
    }
}
