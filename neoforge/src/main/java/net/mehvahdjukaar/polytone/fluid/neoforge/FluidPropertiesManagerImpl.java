package net.mehvahdjukaar.polytone.fluid.neoforge;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.event.entity.player.ClientInformationUpdatedEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

public class FluidPropertiesManagerImpl {

    private static final Map<FluidType, IClientFluidTypeExtensions> FLUID_EXTENSIONS = new HashMap<>();

    public static void tryAddSpecial(Fluid fluid, FluidPropertyModifier colormap) {
        var fluidType = fluid.getFluidType();
        //gets real one. will internally try to get wrapped but a map is empty now
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluidType);
        if (!(ext instanceof FluidExtensionWrapper)) {
            FLUID_EXTENSIONS.put(fluidType, new FluidExtensionWrapper(ext, colormap));
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
        public int getTintColor() {
            var col = modifier.getTint();
            if (col != null) {
                return col.getColor(null, null, null, -1) | 0xff000000;
            }
            return existingProperties.getTintColor();
        }

        @Override
        public int getTintColor(FluidStack stack) {
            var col = modifier.getTint();
            if (col != null) {
                return col.getColor(null, null, null, -1) | 0xff000000;
            }
            return existingProperties.getTintColor();
        }

        @Override
        public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            var col = modifier.getTint();
            if (col != null) {
                return col.getColor(state.createLegacyBlock(), getter, pos, -1) | 0xff000000;
            }
            return existingProperties.getTintColor();
        }

        @Override
        public ResourceLocation getStillTexture() {
            return existingProperties.getStillTexture();
        }

        @Override
        public ResourceLocation getFlowingTexture() {
            return existingProperties.getFlowingTexture();
        }

        @Override
        public @Nullable ResourceLocation getOverlayTexture() {
            return existingProperties.getOverlayTexture();
        }

        @Override
        public @Nullable ResourceLocation getRenderOverlayTexture(Minecraft mc) {
            return existingProperties.getRenderOverlayTexture(mc);
        }

        @Override
        public void renderOverlay(Minecraft mc, PoseStack poseStack) {
            existingProperties.renderOverlay(mc, poseStack);
        }

        @Override
        public @NotNull Vector4f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector4f fluidFogColor) {
            var col = modifier.getFogColormap();
            if (col != null) {
                float[] unpack = ColorUtils.unpack(col.getColor(null, level, null, -1));
                return new Vector4f(unpack[0], unpack[1], unpack[2], fluidFogColor.w);
            }
            return existingProperties.modifyFogColor(camera, partialTick, level, renderDistance, darkenWorldAmount, fluidFogColor);
        }

        @Override
        public FogParameters modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick, FogParameters fogParameters) {
            return existingProperties.modifyFogRender(camera, mode, renderDistance, partialTick, fogParameters);
        }

        @Override
        public ResourceLocation getStillTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            return existingProperties.getStillTexture(state, getter, pos);
        }

        @Override
        public ResourceLocation getFlowingTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            return existingProperties.getFlowingTexture(state, getter, pos);
        }

        @Override
        public ResourceLocation getOverlayTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            return existingProperties.getOverlayTexture(state, getter, pos);
        }

        @Override
        public ResourceLocation getStillTexture(FluidStack stack) {
            return existingProperties.getStillTexture(stack);
        }

        @Override
        public ResourceLocation getOverlayTexture(FluidStack stack) {
            return existingProperties.getOverlayTexture(stack);
        }

        @Override
        public ResourceLocation getFlowingTexture(FluidStack stack) {
            return existingProperties.getFlowingTexture(stack);
        }
    }
}
