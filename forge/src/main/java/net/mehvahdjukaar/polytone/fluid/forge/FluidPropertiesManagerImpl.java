package net.mehvahdjukaar.polytone.fluid.forge;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector3f;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.TintColorGetter;
import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class FluidPropertiesManagerImpl {

    private static final Map<Fluid, FluidAttributes> FLUID_EXTENSIONS = new HashMap<>();

    public static void tryAddSpecial(ResourceLocation id, FluidPropertyModifier colormap) {
        var fluid = getTarget(id, ForgeRegistries.FLUIDS);
        if (fluid != null) {
            var type = fluid.getFirst();

            //gets real one. will internally try to get wrapped but a map is empty now
            FluidAttributes ext = type.getAttributes();
            if(ext instanceof FluidExtensionWrapper){
                Polytone.LOGGER.error("Trying to wrap a wrapper. Something went wrong");
            }

            //create wrapped one
            FLUID_EXTENSIONS.put(type, new FluidExtensionWrapper(type, ext, colormap));
        }
    }

    public static void clearSpecial() {
        FLUID_EXTENSIONS.clear();
    }
    
    @Nullable
    public static <T extends IForgeRegistryEntry<T>> Pair<T, ResourceLocation> getTarget(ResourceLocation resourcePath, IForgeRegistry<T> registry) {
        ResourceLocation id = Polytone.getLocalId(resourcePath);
        var opt = registry.getHolder(id);
        if (opt.isPresent()) return Pair.of(opt.get().value(), id);
        opt = registry.getHolder(resourcePath);
        return opt.map(t -> Pair.of(t.value(), resourcePath)).orElse(null);
    }

    @Nullable
    public static FluidAttributes maybeGetWrappedExtension(Fluid ft) {
        if (!FLUID_EXTENSIONS.isEmpty()) {
            return FLUID_EXTENSIONS.get(ft);
        }
        return null;
    }

    private static class FluidExtensionWrapper extends FluidAttributes {
        FluidAttributes instance;
        FluidPropertyModifier modifier;

        protected FluidExtensionWrapper(Fluid fluid, FluidAttributes instance, FluidPropertyModifier modifier) {
            super(FluidAttributes.builder(new ResourceLocation("a"), new ResourceLocation("b")),
                    fluid);
            this.instance = instance;
            this.modifier  = modifier;
        }

        @Override
        public int getColor() {
            var col = modifier.getColormap();
            if (col != null) {
                return col.getColor(null, null, null, -1) | 0xff000000;
            }
            return instance.getColor();
        }

        @Override
        public int getColor(FluidStack stack) {
            var col = modifier.getColormap();
            if (col != null) {
                return col.getColor(null, null, null, -1) | 0xff000000;
            }
            return instance.getColor();
        }

        @Override
        public int getColor(BlockAndTintGetter level, BlockPos pos) {
            var col = modifier.getColormap();
            if (col != null) {
                return col.getColor(null, level, pos, -1) | 0xff000000;
            }
            return instance.getColor();
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
        public BlockState getBlock(BlockAndTintGetter reader, BlockPos pos, FluidState state) {
            return instance.getBlock(reader, pos, state);
        }

        @Override
        public String getTranslationKey(FluidStack stack) {
            return instance.getTranslationKey(stack);
        }

        @Override
        public String getTranslationKey() {
            return instance.getTranslationKey();
        }

        @Override
        public Stream<ResourceLocation> getTextures() {
            return instance.getTextures();
        }

        @Override
        public SoundEvent getFillSound(BlockAndTintGetter level, BlockPos pos) {
            return instance.getFillSound(level,pos);
        }

        @Override
        public SoundEvent getFillSound(FluidStack stack) {
            return instance.getFillSound(stack);
        }

        @Override
        public SoundEvent getFillSound() {
            return instance.getFillSound();
        }

        @Override
        public SoundEvent getEmptySound(BlockAndTintGetter level, BlockPos pos) {
            return instance.getEmptySound(level,pos);
        }

        @Override
        public SoundEvent getEmptySound(FluidStack stack) {
            return instance.getEmptySound(stack);
        }

        @Override
        public SoundEvent getEmptySound() {
            return instance.getEmptySound();
        }

        @Override
        public ResourceLocation getStillTexture(BlockAndTintGetter level, BlockPos pos) {
            return instance.getStillTexture(level,pos);
        }

        @Override
        public ResourceLocation getFlowingTexture(BlockAndTintGetter level, BlockPos pos) {
            return instance.getFlowingTexture(level,pos);
        }

        @Override
        public Rarity getRarity(BlockAndTintGetter level, BlockPos pos) {
            return instance.getRarity(level,pos);
        }

        @Override
        public Rarity getRarity(FluidStack stack) {
            return instance.getRarity(stack);
        }

        @Override
        public Rarity getRarity() {
            return instance.getRarity();
        }

        @Override
        public ItemStack getBucket(FluidStack stack) {
            return instance.getBucket(stack);
        }

        @Override
        public int getViscosity(BlockAndTintGetter level, BlockPos pos) {
            return instance.getViscosity(level,pos);
        }

        @Override
        public int getViscosity(FluidStack stack) {
            return instance.getViscosity(   stack);
        }

        @Override
        public int getTemperature(BlockAndTintGetter level, BlockPos pos) {
            return instance.getTemperature(level,pos);
        }

        @Override
        public int getTemperature(FluidStack stack) {
            return instance.getTemperature(stack);
        }

        @Override
        public int getLuminosity(BlockAndTintGetter level, BlockPos pos) {
            return instance.getLuminosity(level,pos);
        }

        @Override
        public int getLuminosity(FluidStack stack) {
            return instance.getLuminosity(stack);
        }

        @Override
        public int getDensity(BlockAndTintGetter level, BlockPos pos) {
            return instance.getDensity(level, pos);
        }

        @Override
        public int getDensity(FluidStack stack) {
            return instance.getDensity(stack);
        }

        @Override
        public FluidState getStateForPlacement(BlockAndTintGetter reader, BlockPos pos, FluidStack state) {
            return instance.getStateForPlacement(reader, pos, state);
        }

        @Override
        public Component getDisplayName(FluidStack stack) {
            return instance.getDisplayName(stack);
        }


        @Override
        public ResourceLocation getStillTexture(FluidStack stack) {
            return instance.getStillTexture(stack);
        }


        @Override
        public ResourceLocation getFlowingTexture(FluidStack stack) {
            return instance.getFlowingTexture(stack);
        }
    }
}
