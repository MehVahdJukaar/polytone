package net.mehvahdjukaar.polytone.mixins.fabric;

import net.fabricmc.fabric.impl.client.rendering.ColorResolverRegistryImpl;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(ColorResolverRegistryImpl.class)
public interface ColorResolverRegistryAccessor {

    @Mutable
    @Accessor("ALL_RESOLVERS")
    static void setAllResolvers(Set<ColorResolver> allResolvers) {
        throw new AssertionError();
    }

    @Mutable
    @Accessor("CUSTOM_RESOLVERS")
    static void setCustomResolvers(Set<ColorResolver> customResolvers) {
        throw new AssertionError();
    }

}
