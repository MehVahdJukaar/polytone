package net.mehvahdjukaar.polytone.fluid.fabric;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.content.fluid.FluidPropertyModifier;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;

public class FluidPropertiesManagerImpl {

    public static void tryAddSpecial(Fluid fluid, FluidPropertyModifier fluidMod) {
        var reg = FluidRenderHandlerRegistry.INSTANCE;

        FluidRenderHandler handler = reg.get(fluid);
        if (!(handler instanceof PolytoneFluidRenderHandlerWrapper) && fluidMod.hasColormap()) {
            BlockColor c = fluidMod.getColormap();
            if (c instanceof IColorGetter ccm) {
                c = Polytone.COLORMAPS.getOrCreateConcurrentColormap(ccm);
            }
            reg.register(fluid, new PolytoneFluidRenderHandlerWrapper(handler, c));
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
