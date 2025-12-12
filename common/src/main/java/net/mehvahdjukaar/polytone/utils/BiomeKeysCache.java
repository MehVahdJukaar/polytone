package net.mehvahdjukaar.polytone.utils;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
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

                    //we cant even log here otherwise people will complain
                    //if you are reading this, fix your mod.
                    return Biomes.THE_VOID;
                    //tries with server biomes. This should never happen, server biomes should never be passed here
                   // biomeKey = PlatStuff.getServerRegistryAccess()
                   //         .lookupOrThrow(Registries.BIOME).getResourceKey(biome);

                  //  if (biomeKey.isPresent()) {
                       // return PLAINS;
                        //Polytone.LOGGER.error("Polytone detected a Server Biome was passed to a getColor client side function! This is a bug! Must be caused by some other mod!");
                       // return biomeKey.get();
                 //   } else {
                        //throw new IllegalStateException("Failed to get biome key for biome: " + biome + " This means that biome registry returned an empty key for it. How is this possible? Was it not registered? Seriously HOW? Must be due to some mod doing unsafe stuff!! This is NOT a Polytone issue!");
                 //   }
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
