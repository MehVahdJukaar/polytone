package net.mehvahdjukaar.polytone.fluid.forge;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class FluidPropertiesManagerImpl {

    private static final Map<FluidType, IClientFluidTypeExtensions> FLUID_EXTENSIONS = new HashMap<>();

    public static void tryAddSpecial(ResourceLocation id, FluidPropertyModifier colormap) {
        var fluid = ForgeRegistries.FLUID_TYPES.get().getValue(id);
        if (fluid != null) {
            //gets real one. will internally try to get wrapped but a map is empty now
            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
            if (ext instanceof FluidExtensionWrapper) {
                Polytone.LOGGER.error("Trying to wrap a wrapper. Something went wrong");
            }

            //create wrapped one
            FLUID_EXTENSIONS.put(fluid, new FluidExtensionWrapper(ext, colormap));
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

    private record FluidExtensionWrapper(IClientFluidTypeExtensions instance,
                                         FluidPropertyModifier modifier) implements IClientFluidTypeExtensions {


        @Override
        public int getTintColor() {
            var col = modifier.getColormap();
            if (col != null) {
                return col.getColor(null, null, null, -1) | 0xff000000;
            }
            return instance.getTintColor();
        }

        @Override
        public int getTintColor(FluidStack stack) {
            var col = modifier.getColormap();
            if (col != null) {
                return col.getColor(null, null, null, -1) | 0xff000000;
            }
            return instance.getTintColor();
        }

        @Override
        public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            var col = modifier.getColormap();
            if (col != null) {
                return col.getColor(state.createLegacyBlock(), getter, pos, -1) | 0xff000000;
            }
            return instance.getTintColor();
        }

        @Override
        public ResourceLocation getStillTexture() {
            return instance.getStillTexture();
        }

        @Override
        public ResourceLocation getFlowingTexture() {
            return instance.getFlowingTexture();
        }

        @Override
        public @Nullable ResourceLocation getOverlayTexture() {
            return instance.getOverlayTexture();
        }

        @Override
        public @Nullable ResourceLocation getRenderOverlayTexture(Minecraft mc) {
            return instance.getRenderOverlayTexture(mc);
        }

        @Override
        public void renderOverlay(Minecraft mc, PoseStack poseStack) {
            instance.renderOverlay(mc, poseStack);
        }

        @Override
        public @NotNull Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
            return instance.modifyFogColor(camera, partialTick, level, renderDistance, darkenWorldAmount, fluidFogColor);
        }

        @Override
        public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape) {
            instance.modifyFogRender(camera, mode, renderDistance, partialTick, nearDistance, farDistance, shape);
        }

        @Override
        public ResourceLocation getStillTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            return instance.getStillTexture(state, getter, pos);
        }

        @Override
        public ResourceLocation getFlowingTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            return instance.getFlowingTexture(state, getter, pos);
        }

        @Override
        public ResourceLocation getOverlayTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            return instance.getOverlayTexture(state, getter, pos);
        }

        @Override
        public ResourceLocation getStillTexture(FluidStack stack) {
            return instance.getStillTexture(stack);
        }

        @Override
        public ResourceLocation getOverlayTexture(FluidStack stack) {
            return instance.getOverlayTexture(stack);
        }

        @Override
        public ResourceLocation getFlowingTexture(FluidStack stack) {
            return instance.getFlowingTexture(stack);
        }
    }
}
