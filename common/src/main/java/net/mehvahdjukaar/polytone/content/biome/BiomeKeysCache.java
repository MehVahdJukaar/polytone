package net.mehvahdjukaar.polytone.content.biome;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.jetbrains.annotations.NotNull;

public class BiomeKeysCache {

    private static final ThreadLocal<Object2ObjectOpenHashMap<Biome, ResourceKey<Biome>>> CACHE =
            ThreadLocal.withInitial(Object2ObjectOpenHashMap::new);


    public static ResourceKey<Biome> get(@NotNull Biome biome) {
        var k = CACHE.get().get(biome);
        if (k == null) {
            Level level = Minecraft.getInstance().level;
            if (level == null) return Biomes.PLAINS;
            return CACHE.get().computeIfAbsent(biome, b ->
            {
                var biomeKey = level.registryAccess().lookupOrThrow(Registries.BIOME).getResourceKey(biome);
                if (biomeKey.isEmpty()) {
                    var server = Minecraft.getInstance().getSingleplayerServer();
                    boolean isServerBiome = server != null && server.registryAccess().lookupOrThrow(Registries.BIOME).getResourceKey(biome).isPresent();
                    if (isServerBiome) {
                        throw new ModThrewInServerSideBiomeException("A server side Biome was passed to a client side color getter! This is NOT a Polytone issue. Some other mod is calling client code with server biomes. Biome: " + biome);
                    }

                    //we cant even log here otherwise people will complain
                    //if you are reading this, fix your mod.
                    return Biomes.THE_VOID;
                }
                return biomeKey.get();
            });
        }
        return k;
    }

    public static void clear() {
        CACHE.get().clear();
    }

    public static class ModThrewInServerSideBiomeException extends IllegalStateException {
        public ModThrewInServerSideBiomeException(String message) {
            super(message);
        }
    }

}
