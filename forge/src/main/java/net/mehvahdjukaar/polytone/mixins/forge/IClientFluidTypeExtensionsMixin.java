package net.mehvahdjukaar.polytone.mixins.forge;

import net.mehvahdjukaar.polytone.fluid.forge.FluidPropertiesManagerImpl;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Fluid.class)
public class IClientFluidTypeExtensionsMixin {

    @Inject(method = "getAttributes",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void polytone$wrapToModifyTint(CallbackInfoReturnable<FluidAttributes> cir) {
        var wrapped = FluidPropertiesManagerImpl.maybeGetWrappedExtension((Fluid) (Object)this);
        if (wrapped != null) {
            cir.setReturnValue(wrapped);
        }

    }
}
