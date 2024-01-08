package net.mehvahdjukaar.polytone.mixins.fabric;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.mehvahdjukaar.polytone.properties.colormap.Colormap;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ColorResolverMixin {

    @Shadow public abstract int calculateBlockTint(BlockPos blockPos, ColorResolver colorResolver);

    @Inject(method = "method_23778", at = @At("TAIL"))
    public void polytone$addExtraResolvers(Object2ObjectArrayMap<ColorResolver, BlockTintCache> map, CallbackInfo ci){
        map.put(Colormap.TEMPERATURE_RESOLVER, new BlockTintCache((blockPos) ->
                this.calculateBlockTint(blockPos, Colormap.TEMPERATURE_RESOLVER)));
        map.put(Colormap.DOWNFALL_RESOLVER, new BlockTintCache((blockPos) ->
                this.calculateBlockTint(blockPos, Colormap.DOWNFALL_RESOLVER)));
    }
}
