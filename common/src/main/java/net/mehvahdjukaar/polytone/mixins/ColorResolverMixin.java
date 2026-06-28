package net.mehvahdjukaar.polytone.mixins;

import net.minecraft.world.phys.Vec3;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
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

@Mixin(ClientLevel.class)
public abstract class ColorResolverMixin extends Level {

    @Shadow @Final @Mutable
    private Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches;

    @Shadow
    public abstract int calculateBlockTint(BlockPos blockPos, ColorResolver colorResolver);

    protected ColorResolverMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, boolean bl, boolean bl2, long l, int i) {
        super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
    }

    /**
     * Hack so we don't have to register these on every reload. They are instead added on request.
     * <p>
     * We handle ANY missing resolver, not just our {@link Colormap}s: because we swap the static
     * {@code BiomeColors.WATER/GRASS/FOLIAGE_COLOR_RESOLVER} fields, a ClientLevel built while a
     * colormap was active keys its fixed {@code tintCaches} slot on that colormap. Once the colormap
     * is removed (and the vanilla resolver restored) on reload, that slot would be left with no entry,
     * causing a null cache - which on Fabric becomes a hard crash from its strict {@code modifyNullCache}.
     * Lazily re-creating the vanilla cache here keeps the map complete regardless of swap timing.
     */
    @Inject(method = "getBlockTint", at = @At("HEAD"))
    private void polytone$makeCachesForColormaps(BlockPos pos, ColorResolver resolver, CallbackInfoReturnable<Integer> info) {
        if (this.tintCaches.containsKey(resolver)) return;
        BlockTintCache cache;
        if (resolver instanceof Colormap c) {
            cache = new BlockTintCache(p -> c.calculateBlendedColor((ClientLevel) (Object) this, Vec3.atCenterOf(p)));
        } else if (resolver == BiomeColors.GRASS_COLOR_RESOLVER
                || resolver == BiomeColors.FOLIAGE_COLOR_RESOLVER
                || resolver == BiomeColors.WATER_COLOR_RESOLVER) {
            cache = new BlockTintCache(p -> this.calculateBlockTint(p, resolver));
        } else {
            return;
        }
        //make copy of the map and assigns it as it has limited capacity
        var newMap = new Object2ObjectArrayMap<>(this.tintCaches);
        newMap.put(resolver, cache);
        this.tintCaches = newMap;
    }

    /**
     * Remove all custom added resolvers
     */
    @Inject(method = "clearTintCaches", at = @At("HEAD"))
    private void polytone$resetCustomColorResolvers(CallbackInfo info) {
        this.tintCaches.entrySet().removeIf(entry -> entry.getKey() instanceof Colormap);
    }
}
