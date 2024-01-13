package net.mehvahdjukaar.polytone.biome;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.PartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener.scanDirectory;

public class BiomeEffectsManager extends JsonPartialReloader {

    private final Map<ResourceLocation, BiomeSpecialEffects> vanillaEffects = new HashMap<>();

    private final Map<ResourceLocation, BiomeEffectModifier> effectsToApply = new HashMap<>();

    public BiomeEffectsManager(){
        super("biome_effects");
    }

    @Override
    public void process(Map<ResourceLocation, JsonElement> biomesJsons) {
        for (var j : biomesJsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            BiomeEffectModifier effect = BiomeEffectModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Biome Special Effect with json id {} - error: {}", id, errorMsg))
                    .getFirst();

            effectsToApply.put(id, effect);
        }
    }

    @Override
    public void apply() {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            doApply(level.registryAccess(), false);
        }
        //else apply as soon as we load a level
    }

    public void doApply(RegistryAccess registryAccess, boolean firstLogin) {
        if (firstLogin) vanillaEffects.clear();

        Registry<Biome> biomeReg = registryAccess.registry(Registries.BIOME).get();
        for (var v : effectsToApply.entrySet()) {

            var biome = Polytone.getTarget(v.getKey(), biomeReg);
            if (biome != null) {
                var old = v.getValue().apply(biome.getFirst());

                vanillaEffects.put(biome.getSecond(), old);
            }
        }
        if (!vanillaEffects.isEmpty())
            Polytone.LOGGER.info("Applied {} Custom Biome Effects Properties", vanillaEffects.size());
        //we dont clear effects to apply because we need to re apply on world reload
    }

    @Override
    public void reset() {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            Registry<Biome> biomeReg = level.registryAccess().registry(Registries.BIOME).get();
            for (var v : vanillaEffects.entrySet()) {
                var biome = biomeReg.getOptional(v.getKey());
                biome.ifPresent(value -> value.specialEffects = v.getValue());
            }
            //reset all
        }
        //if we don't have a level, biomes don't exist anymore, so we don't care

        vanillaEffects.clear();

        //whatever happens we always clear stuff to apply
        effectsToApply.clear();
    }


}
