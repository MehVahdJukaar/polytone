package net.mehvahdjukaar.polytone.fluid.fabric;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;

public class FluidPropertiesManagerImpl {

    public static void tryAddSpecial(Fluid fluid, FluidPropertyModifier colormap) {
        var reg = FluidRenderHandlerRegistry.INSTANCE;

        FluidRenderHandler handler = reg.get(fluid);
        if (!(handler instanceof PolytoneFluidRenderHandlerWrapper)) {
            reg.register(fluid, new PolytoneFluidRenderHandlerWrapper(handler, colormap));
        }
    }

    public static void clearSpecial() {
        var reg = FluidRenderHandlerRegistry.INSTANCE;

        for (var f : BuiltInRegistries.FLUID) {
            FluidRenderHandler handler = reg.get(f);
            if (handler instanceof PolytoneFluidRenderHandlerWrapper wrapper) {
                reg.register(f, wrapper.instance());
            }
        }
    }
}
