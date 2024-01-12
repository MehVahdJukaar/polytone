package net.mehvahdjukaar.polytone.mixins.fabric;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.impl.client.rendering.fluid.FluidRenderHandlerRegistryImpl;
import net.mehvahdjukaar.polytone.fabric.PolytoneFluidRenderHandlerWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = FluidRenderHandlerRegistryImpl.class, remap = false)
public abstract class FluidRendererHandlerRegistryMixin {

    @ModifyVariable(method = "register", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private FluidRenderHandler polytone$wrapRenderHandler(FluidRenderHandler instance) {
        return new PolytoneFluidRenderHandlerWrapper(instance);
    }
}
