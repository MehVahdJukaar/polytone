package net.mehvahdjukaar.polytone.biome;

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
import net.minecraft.world.level.biome.BiomeSpecialEffects;

import java.util.HashMap;
import java.util.Map;

public class BiomeEffectsManager {


    private static final Map<ResourceLocation, BiomeSpecialEffects> VANILLA_EFFECTS = new HashMap<>();

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
            doApply(level.registryAccess(), false);
        }
        //else apply as soon as we load a level
    }

    public static void doApply(RegistryAccess registryAccess, boolean firstLogin) {
        if (firstLogin) VANILLA_EFFECTS.clear();

        Registry<Biome> biomeReg = registryAccess.registry(Registries.BIOME).get();
        for (var v : EFFECTS_TO_APPLY.entrySet()) {

            var biome = Polytone.getTarget(v.getKey(), biomeReg);
            if (biome != null) {
                var old = v.getValue().apply(biome.getFirst());

                VANILLA_EFFECTS.put(biome.getSecond(), old);
            }
        }
        if (!VANILLA_EFFECTS.isEmpty())
            Polytone.LOGGER.info("Applied {} Custom Biome Effects Properties", VANILLA_EFFECTS.size());
        //we dont clear effects to apply because we need to re apply on world reload
    }

    public static void reset() {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            Registry<Biome> biomeReg = level.registryAccess().registry(Registries.BIOME).get();
            for (var v : VANILLA_EFFECTS.entrySet()) {
                var biome = biomeReg.getOptional(v.getKey());
                biome.ifPresent(value -> value.specialEffects = v.getValue());
            }
            //reset all
        }
        //if we don't have a level, biomes don't exist anymore, so we don't care

        VANILLA_EFFECTS.clear();

        //whatever happens we always clear stuff to apply
        EFFECTS_TO_APPLY.clear();
    }

    private static void resetToVanilla() {

    }

}
