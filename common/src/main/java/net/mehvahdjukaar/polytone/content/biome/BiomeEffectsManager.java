package net.mehvahdjukaar.polytone.content.biome;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.misc.Parsed;
import net.mehvahdjukaar.polytone.misc.reloader.JsonPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;

public class BiomeEffectsManager extends JsonPartialReloader {

    private final Map<Biome, BiomeEffectModifier> vanillaEffects = new HashMap<>();
    private final Map<Biome, BiomeEffectModifier> effectsToApply = new HashMap<>();

    public BiomeEffectsManager() {
        super("biome_modifiers", "biome_effects");
    }

    @Override
    public void parseWithLevel(Map<Identifier, JsonElement> jsons, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        for (var v : Parsed.batchParseOnlyEnabled(jsons, BiomeEffectModifier.CODEC, ops, "biome modifier")) {
            addEffect(v.getKey(), v.getValue(), access);
        }
    }

    private void addEffect(Identifier pathId, BiomeEffectModifier mod, HolderLookup.Provider access) {
        HolderLookup.RegistryLookup<Biome> registry = access.lookupOrThrow(Registries.BIOME);
        for (var biome : mod.targets().compute(pathId, registry)) {
            effectsToApply.merge(biome.value(), mod, BiomeEffectModifier::merge);
        }
    }

    // we need registry ops here since special effects use registry stuff...
    @Override
    public void applyWithLevel(HolderLookup.Provider registryAccess, boolean isLogIn) {

        if (isLogIn) vanillaEffects.clear();

        for (var v : effectsToApply.entrySet()) {
            Biome biome = v.getKey();
            BiomeEffectModifier modifier = v.getValue();
            BiomeEffectModifier old = modifier.apply(biome);

            vanillaEffects.put(biome, old);
        }
        if (!vanillaEffects.isEmpty()) {
            Polytone.LOGGER.info("Applied {} Custom Biome Effects Properties", vanillaEffects.size());
        }
        //we don't clear effects to apply because we need to re apply on world reload
    }

    @Override
    public void resetWithLevel(boolean isLogOff) {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            for (var v : vanillaEffects.entrySet()) {
                Biome biome = v.getKey();
                BiomeEffectModifier biomeModifier = v.getValue();
                biomeModifier.apply(biome);
            }
            //reset all
        }
        //if we don't have a level, biomes don't exist anymore, so we don't care

        vanillaEffects.clear();

        //whatever happens, we always clear stuff to apply
        effectsToApply.clear();
    }

    public boolean hasModifiedAttributes() {
        return effectsToApply.values().stream().anyMatch(
                m -> !m.environmentAttributesMod().isEmpty()
        );
    }
}
