package net.mehvahdjukaar.polytone.mixins.fabric;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import net.fabricmc.fabric.impl.client.rendering.ColorResolverRegistryImpl;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(ClientLevel.class)
public abstract class ClientLevelFabricMixin {

    @Shadow
    public abstract int calculateBlockTint(BlockPos blockPos, ColorResolver colorResolver);

    @Unique
    private Field fabricCacheField = null;

    @Inject(method = "clearTintCaches", at = @At("RETURN"))
    private void clearTintCaches(CallbackInfo ci) {
        if (fabricCacheField == null) {
            try {
                fabricCacheField = ClientLevel.class.getDeclaredField("customColorCache");
                fabricCacheField.setAccessible(true);
            } catch (Exception e) {
                try {
                    for (var f : ClientLevel.class.getDeclaredFields()) {
                        if (f.getType().isAssignableFrom(Reference2ReferenceMap.class)) {
                            fabricCacheField = f;
                            fabricCacheField.setAccessible(true);
                            break;
                        }
                    }
                } catch (Exception ee) {
                    throw new RuntimeException(ee);
                }
            }

        }
        try {
            //re-assigns fields. Hoping other mods aren't adding shit to it
            fabricCacheField.set(this, ColorResolverRegistryImpl.createCustomCacheMap(resolver ->
                    new BlockTintCache(pos -> calculateBlockTint(pos, resolver))));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
