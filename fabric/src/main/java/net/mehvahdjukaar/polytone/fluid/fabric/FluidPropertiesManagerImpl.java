package net.mehvahdjukaar.polytone.fluid.fabric;

import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.Map;

public class FluidPropertiesManagerImpl {

    private static final Map<Fluid, FluidPropertyModifier> FLUID_COLORMAPS = new HashMap<>();


    public static void tryAddSpecial(ResourceLocation id, FluidPropertyModifier colormap) {
    }

    public static void clearSpecial() {
    }
}
