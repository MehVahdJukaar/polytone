package net.mehvahdjukaar.polytone.mixins;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
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
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(ClientLevel.class)
public abstract class ColorResolverMixin extends Level {

    @Shadow @Final @Mutable
    private Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches;

    @Shadow
    public abstract int calculateBlockTint(BlockPos blockPos, ColorResolver colorResolver);

    protected ColorResolverMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l, int i) {
        super(writableLevelData, resourceKey, registryAccess, holder, supplier, bl, bl2, l, i);
    }

    // Registers these on request instead of on every reload. We handle ANY missing resolver, not just
    // our colormaps: swapping the static BiomeColors.*_COLOR_RESOLVER fields means a ClientLevel built
    // while a colormap was active keys its fixed tintCaches slot on that colormap, so once the colormap
    // is removed on reload that slot is left with no entry - a null cache, which fabric's strict
    // modifyNullCache turns into a hard crash. Re-creating the vanilla cache lazily keeps the map whole.
    @Inject(method = "getBlockTint", at = @At("HEAD"))
    private void polytone$makeCachesForColormaps(BlockPos pos, ColorResolver resolver, CallbackInfoReturnable<Integer> info) {
        if (this.tintCaches.containsKey(resolver)) return;
        BlockTintCache cache;
        if (resolver instanceof Colormap c) {
            cache = new BlockTintCache(p -> c.calculateBlendedColor(this, p));
        } else if (resolver == BiomeColors.GRASS_COLOR_RESOLVER
                || resolver == BiomeColors.FOLIAGE_COLOR_RESOLVER
                || resolver == BiomeColors.WATER_COLOR_RESOLVER) {
            // One of the 3 vanilla resolvers was orphaned from this level's fixed tintCaches because we
            // swap the BiomeColors static fields (Fluid/BlockPropertiesManager): a level built while a
            // colormap held the slot loses it once the colormap is removed on reload. Re-seed it so the
            // restored vanilla resolver still resolves. Any OTHER (third party registered) resolver is
            // intentionally left to the platform's own registered-resolver cache.
            cache = new BlockTintCache(p -> this.calculateBlockTint(p, resolver));
        } else {
            return;
        }
        //make copy of the map and assigns it as it has limited capacity
        var newMap = new Object2ObjectArrayMap<>(this.tintCaches);
        newMap.put(resolver, cache);
        this.tintCaches = newMap;
    }

    @Inject(method = "clearTintCaches", at = @At("HEAD"))
    private void polytone$resetCustomColorResolvers(CallbackInfo info) {
        this.tintCaches.entrySet().removeIf(entry -> entry.getKey() instanceof Colormap);
    }
}
