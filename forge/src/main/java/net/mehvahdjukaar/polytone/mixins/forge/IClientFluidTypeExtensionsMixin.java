package net.mehvahdjukaar.polytone.mixins.forge;

import net.mehvahdjukaar.polytone.fluid.forge.FluidPropertiesManagerImpl;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IClientFluidTypeExtensions.class, remap = false)
public interface IClientFluidTypeExtensionsMixin {

    @Inject(method = "of(Lnet/minecraftforge/fluids/FluidType;)Lnet/minecraftforge/client/extensions/common/IClientFluidTypeExtensions;",
            at = @At("HEAD"), cancellable = true)
    private static void polytone$wrapToModifyTint(FluidType type, CallbackInfoReturnable<IClientFluidTypeExtensions> cir) {
        var wrapped = FluidPropertiesManagerImpl.maybeGetWrappedExtension(type);
        if (wrapped != null) {
            cir.setReturnValue(wrapped);
        }

    }
}
