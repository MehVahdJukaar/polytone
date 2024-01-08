package net.mehvahdjukaar.polytone.properties.biome;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;

public class BiomeEffectsManager {


    private static final Map<ResourceLocation, BiomeEffectModifier> VANILLA_EFFECTS = new HashMap<>();

    private static final Map<ResourceLocation, BiomeEffectModifier> EFFECTS_TO_APPLY = new HashMap<>();


    public static void process(Map<ResourceLocation, JsonElement> biomesJsons) {
        for (var j : biomesJsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            BiomeEffectModifier effect = BiomeEffectModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Biome Special Effect with json id {} - error: {}", id, errorMsg))
                    .getFirst();

            EFFECTS_TO_APPLY.put(id, effect);
        }
    }

    public static void tryApply() {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            doApply(level.registryAccess());
        }
        //else apply as soon as we load a level
    }

    public static void doApply(RegistryAccess registryAccess) {
        Registry<Biome> biomeReg = registryAccess.registry(Registries.BIOME).get();
        for (var v : EFFECTS_TO_APPLY.entrySet()) {
            ResourceLocation id = v.getKey();
            var biome = biomeReg.getOptional(id);
            if (biome.isPresent()) {
                var old = v.getValue().apply(biome.get());

                VANILLA_EFFECTS.put(id, old);
            }
        }
        // we clear so we don't apply multiple times
        EFFECTS_TO_APPLY.clear();
    }

    public static void reset() {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            Registry<Biome> biomeReg = level.registryAccess().registry(Registries.BIOME).get();
            for(var v : VANILLA_EFFECTS.entrySet()){
                var biome =  biomeReg.getOptional(v.getKey());
                biome.ifPresent(value -> v.getValue().apply(value));
            }
            //reset all
        }
        //if we don't have a level, biomes don't exist anymore, so we don't care

        VANILLA_EFFECTS.clear();
    }

}
