package net.mehvahdjukaar.polytone.mixins;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(ClientLevel.class)
public abstract class ColorResolverMixin extends Level {

    @Shadow @Final private Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches;

    protected ColorResolverMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, boolean bl, boolean bl2, long l, int i) {
        super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
    }

    /**
     * Hack so we don't have to register these on every reload. They are instead added on request
     */
    @Inject(method = "getBlockTint", at = @At("HEAD"))
    private void fixVanillaColorCache(BlockPos pos, ColorResolver resolver, CallbackInfoReturnable<Integer> info) {
        if (!this.tintCaches.containsKey(resolver) && resolver instanceof Colormap c) {
            tintCaches.put(resolver, new BlockTintCache(p -> c.calculateBlendedColor(this, p)));
        }
    }

    /**
     * Remove all custom added resolvers
     */
    @Inject(method = "clearTintCaches", at = @At("RETURN"))
    private void polytone$resetCustomColorResolvers(CallbackInfo info) {
        this.tintCaches.entrySet().removeIf(entry -> entry.getKey() instanceof Colormap);
    }
}
