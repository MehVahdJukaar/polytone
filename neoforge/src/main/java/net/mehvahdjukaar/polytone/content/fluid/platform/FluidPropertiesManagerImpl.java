package net.mehvahdjukaar.polytone.content.fluid.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.content.fluid.FluidPropertyModifier;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

public class FluidPropertiesManagerImpl {

    private static final Map<FluidType, IClientFluidTypeExtensions> FLUID_EXTENSIONS = new HashMap<>();

    public static void tryAddSpecial(Fluid fluid, FluidPropertyModifier prop) {
        FluidType fluidType = fluid.getFluidType();
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluidType);
        if (!(ext instanceof FluidExtensionWrapper)) {
            IColorGetter fogColormap = prop.getFogColormap();
            FLUID_EXTENSIONS.put(fluidType, new FluidExtensionWrapper(ext, fogColormap));
        }
    }

    public static void clearSpecial() {
        FLUID_EXTENSIONS.clear();
    }

    @Nullable
    public static IClientFluidTypeExtensions maybeGetWrappedExtension(FluidType ft) {
        if (!FLUID_EXTENSIONS.isEmpty()) {
            return FLUID_EXTENSIONS.get(ft);
        }
        return null;
    }

    private record FluidExtensionWrapper(IClientFluidTypeExtensions existingProperties,
                                         @Nullable IColorGetter fogColor) implements IClientFluidTypeExtensions {

        @Override
        public @Nullable Identifier getRenderOverlayTexture(Minecraft mc) {
            return existingProperties.getRenderOverlayTexture(mc);
        }

        @Override
        public void renderOverlay(Minecraft mc, PoseStack poseStack, MultiBufferSource buffers) {
            existingProperties.renderOverlay(mc, poseStack, buffers);
        }

        @Override
        public void modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector4f fluidFogColor) {
            if (fogColor != null) {
                float[] unpack = ColorUtils.unpack(fogColor.sampleColor(null, null, null, null, null));
                fluidFogColor.set(unpack[0], unpack[1], unpack[2], fluidFogColor.w);
            } else {
                existingProperties.modifyFogColor(camera, partialTick, level, renderDistance, darkenWorldAmount, fluidFogColor);
            }
        }
    }
}
