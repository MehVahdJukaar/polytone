package net.mehvahdjukaar.polytone.content.biome;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.attributes.IExtendedInterpolator;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class BiomeEffectsManager extends ContentManager<BiomeEffectModifier> {

    private final Map<Biome, BiomeEffectModifier> vanillaEffects = new HashMap<>();
    private final Map<Biome, BiomeEffectModifier> effectsToApply = new HashMap<>();
    private final Set<EnvironmentAttribute<?>> alteredAttributes = new HashSet<>();

    private boolean hasPostAttributes = false;

    public BiomeEffectsManager() {
        // wrap(CODEC), not DIRECT_CODEC: files parse through the postProcess wrapper, and the
        // editor must validate through the same one.
        super(Spec.of("Biome modifier", () -> SchemaCodec.wrap(BiomeEffectModifier.CODEC))
                .wikiPage("Biome-Effect-Modifiers")
                .folders("biome_modifiers", "biome_effects"));
    }

    @Override
    public void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        Map<Identifier, JsonElement> jsons = resources.jsons();
        for (var v : parseEnabledJsons(jsons, ops)) {
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

        for (var m : effectsToApply.values()) {
            this.alteredAttributes.addAll(m.attributeModifications().getAllModifiedAttributes());
        }

        this.hasPostAttributes = effectsToApply.values().stream().anyMatch(
                m -> !m.attributeModifications().postProcess().isEmpty()
        );
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

        hasPostAttributes = false;
        alteredAttributes.clear();
    }

    public boolean hasModifiedAttributes() {
        return !alteredAttributes.isEmpty();
    }


    public boolean hasPostAttributes() {
        return hasPostAttributes;
    }

    public void addPostLayers(EnvironmentAttributeSystem.Builder builder, Level level) {
        RegistryAccess registryAccess = level.registryAccess();
        BiomeManager biomeManager = level.getBiomeManager();
        addBiomeLayer(builder, registryAccess.lookupOrThrow(Registries.BIOME), biomeManager);
    }

    //same as base biome layer
    private void addBiomeLayer(EnvironmentAttributeSystem.Builder builder, HolderLookup<Biome> holderLookup,
                               BiomeManager biomeManager) {
        Stream<EnvironmentAttribute<?>> allAttributesInPost = effectsToApply.entrySet().stream().flatMap(
                e -> e.getValue().getPostProcessAttributes().keySet().stream()
        ).distinct();

        allAttributesInPost.forEach((environmentAttribute) -> addBiomeLayerForAttribute(builder,
                environmentAttribute, biomeManager));
    }

    public boolean doesAttributeNeedSpatialInterpolation(EnvironmentAttribute<?> attr) {
        //pessimistic approach. suboptimal, will cause extra interpolation many times
        return alteredAttributes.contains(attr);
    }

    private <Value> void addBiomeLayerForAttribute(EnvironmentAttributeSystem.Builder builder,
                                                   EnvironmentAttribute<Value> environmentAttribute,
                                                   BiomeManager biomeManager) {
        builder.addPositionalLayer(environmentAttribute, (object, vec3, spatialAttributeInterpolator) -> {
            if (spatialAttributeInterpolator != null
                //&& environmentAttribute.isSpatiallyInterpolated()
                //all are spatially interpolated now since they possibly could be
            ) {
                spatialAttributeInterpolator = ((IExtendedInterpolator) spatialAttributeInterpolator)
                        .polytone$getOrCreatePostInterpolator();

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

    public EnvironmentAttributeMap getPostAttributes(Biome value) {
        BiomeEffectModifier modifier = effectsToApply.get(value);
        if (modifier != null) {
            return modifier.getPostProcessAttributes();
        }
        return EnvironmentAttributeMap.EMPTY;
    }

}
