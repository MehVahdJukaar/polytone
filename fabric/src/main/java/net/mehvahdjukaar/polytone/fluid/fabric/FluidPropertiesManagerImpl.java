package net.mehvahdjukaar.polytone.fluid.fabric;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.Map;

public class FluidPropertiesManagerImpl {

    public static void tryAddSpecial(ResourceLocation id, FluidPropertyModifier colormap) {
        var fluid = BuiltInRegistries.FLUID.getOptional(id);
        var reg = FluidRenderHandlerRegistry.INSTANCE;

        if(fluid.isPresent()){
            FluidRenderHandler handler = reg.get(fluid.get());
            if(!(handler instanceof PolytoneFluidRenderHandlerWrapper)){
                reg.register(fluid.get(), new PolytoneFluidRenderHandlerWrapper(handler, colormap));
            }
        }
    }

    public static void clearSpecial() {
       var reg = FluidRenderHandlerRegistry.INSTANCE;

        for(var f : BuiltInRegistries.FLUID){
            FluidRenderHandler handler = reg.get(f);
            if(handler instanceof PolytoneFluidRenderHandlerWrapper wrapper){
                reg.register(f, wrapper.instance());
            }
        }
    }
}
