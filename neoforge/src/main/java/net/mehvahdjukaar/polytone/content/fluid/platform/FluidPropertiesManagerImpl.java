package net.mehvahdjukaar.polytone.content.fluid.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.content.fluid.FluidPropertyModifier;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.core.BlockPos;
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
        if (prop.getFogColormap() == null && !prop.hasFogShape()) return;
        FluidType fluidType = fluid.getFluidType();
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluidType);
        if (!(ext instanceof FluidExtensionWrapper)) {
            FLUID_EXTENSIONS.put(fluidType, new FluidExtensionWrapper(ext, prop));
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
                                         FluidPropertyModifier modifier) implements IClientFluidTypeExtensions {

        @Override
        public @Nullable Identifier getRenderOverlayTexture(Minecraft mc) {
            return existingProperties.getRenderOverlayTexture(mc);
        }

        @Override
        public void renderOverlay(Minecraft mc, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
            existingProperties.renderOverlay(mc, poseStack, submitNodeCollector);
        }

        @Override
        public void modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector4f fluidFogColor) {
            IColorGetter fogColor = modifier.getFogColormap();
            if (fogColor == null) {
                existingProperties.modifyFogColor(camera, partialTick, level, renderDistance, darkenWorldAmount, fluidFogColor);
                return;
            }
            BlockPos pos = camera.blockPosition();
            float[] unpack = ColorUtils.unpack(fogColor.colorInWorld(level.getBlockState(pos), level, pos));
            fluidFogColor.set(unpack[0], unpack[1], unpack[2], fluidFogColor.w);
        }

        @Override
        public void modifyFogRender(Camera camera, FogEnvironment environment, float renderDistance, float partialTick, FogData fogData) {
            existingProperties.modifyFogRender(camera, environment, renderDistance, partialTick, fogData);
            if (modifier.hasFogShape()) modifier.modifyFogShape(fogData, camera, Minecraft.getInstance().level);
        }
    }
}
