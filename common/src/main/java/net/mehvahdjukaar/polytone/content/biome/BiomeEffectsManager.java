package net.mehvahdjukaar.polytone.content.biome;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.reloader.JsonPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

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
                m -> !m.attributeModifications().isEmpty()
        );
    }

    public void addPostLayers(EnvironmentAttributeSystem.Builder builder, Level level) {
        RegistryAccess registryAccess = level.registryAccess();
        BiomeManager biomeManager = level.getBiomeManager();
        addBiomeLayer(builder, registryAccess.lookupOrThrow(Registries.BIOME), biomeManager);
    }

    //same as base biome layer
    private void addBiomeLayer(EnvironmentAttributeSystem.Builder builder, HolderLookup<Biome> holderLookup, BiomeManager biomeManager) {
        Stream<EnvironmentAttribute<?>> allAttributesInPost = effectsToApply.entrySet().stream().flatMap(
                e -> e.getValue().getPostProcessAttributes().keySet().stream()
        ).distinct();

        allAttributesInPost.forEach((environmentAttribute) -> addBiomeLayerForAttribute(builder, environmentAttribute, biomeManager));
    }

    private <Value> void addBiomeLayerForAttribute(EnvironmentAttributeSystem.Builder builder, EnvironmentAttribute<Value> environmentAttribute,
                                                   BiomeManager biomeManager) {
        builder.addPositionalLayer(environmentAttribute, (object, vec3, spatialAttributeInterpolator) -> {
            if (spatialAttributeInterpolator != null && environmentAttribute.isSpatiallyInterpolated()) {
                return spatialAttributeInterpolator.applyAttributeLayer(environmentAttribute, object);
            } else {
                Holder<Biome> holder = biomeManager.getNoiseBiomeAtPosition(vec3.x, vec3.y, vec3.z);
                BiomeEffectModifier biomeEffectModifier = effectsToApply.get(holder.value());
                if (biomeEffectModifier == null) return object;
                EnvironmentAttributeMap posMap = biomeEffectModifier.getPostProcessAttributes();
                return posMap.applyModifier(environmentAttribute, object);
            }
        });
    }

}
