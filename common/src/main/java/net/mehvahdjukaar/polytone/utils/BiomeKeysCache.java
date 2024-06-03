package net.mehvahdjukaar.polytone.utils;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public class BiomeKeysCache {

    private static final ResourceKey<Biome> PLAINS = ResourceKey.create(Registries.BIOME, new ResourceLocation("minecraft:plains"));

    private static final Object2ObjectOpenHashMap<Biome, ResourceKey<Biome>> CACHE = new Object2ObjectOpenHashMap<>();

    public static ResourceKey<Biome> get(Biome biome) {
        var k = CACHE.get(biome);
        if (k == null) {
            Level level = Minecraft.getInstance().level;
            if (level == null) return PLAINS;
            return CACHE.computeIfAbsent(biome, b -> level.registryAccess().registryOrThrow(Registries.BIOME)
                    .getResourceKey(biome).get());
        }
        return k;
    }

    public static void clear() {
        CACHE.clear();
    }

    ;
}
