package net.mehvahdjukaar.polytone.utils;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public class BiomeKeysCache {

    private static final ResourceKey<Biome> PLAINS = ResourceKey.create(Registries.BIOME, new ResourceLocation("minecraft:plains"));

    private static final ThreadLocal<Object2ObjectOpenHashMap<Biome, ResourceKey<Biome>>> CACHE =
            ThreadLocal.withInitial(Object2ObjectOpenHashMap::new);

    public static ResourceKey<Biome> get(Biome biome) {
        var k = CACHE.get().get(biome);
        if (k == null) {
            Level level = Minecraft.getInstance().level;
            if (level == null) return PLAINS;
            return CACHE.get().computeIfAbsent(biome, b ->
            {
                var biomeKey = level.registryAccess().registryOrThrow(Registries.BIOME).getResourceKey(biome);
                if (biomeKey.isEmpty()) {

                    //tries with server biomes. This should never happen, server biomes should never be passed here
                    biomeKey = PlatStuff.getServerRegistryAccess()
                            .registryOrThrow(Registries.BIOME).getResourceKey(biome);

                    if (biomeKey.isPresent()) {
                        Polytone.LOGGER.error("Polytone detected a Server Biome was passed to a getColor client side function! This is a bug! Must be caused by some other mod!");
                        return biomeKey.get();
                    } else {
                        throw new IllegalStateException("Failed to get biome key for biome: " + biome + " This means that biome registry returned an empty key for it. How is this possible? Was it not registered? Seriously HOW? Must be due to some mod doing unsafe stuff!! This is NOT a Polytone issue!");
                    }
                }
                return biomeKey.get();
            });
        }
        return k;
    }

    public static void clear() {
        CACHE.get().clear();
    }

}
