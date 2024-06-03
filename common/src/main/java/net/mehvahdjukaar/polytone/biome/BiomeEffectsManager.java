package net.mehvahdjukaar.polytone.biome;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.material.Fluids;

import java.util.HashMap;
import java.util.Map;

public class BiomeEffectsManager extends JsonPartialReloader {

    private final Map<ResourceLocation, BiomeSpecialEffects> vanillaEffects = new HashMap<>();

    private final Map<ResourceLocation, BiomeEffectModifier> effectsToApply = new HashMap<>();
    private boolean needsDynamicApplication = true;

    public BiomeEffectsManager() {
        super("biome_modifiers", "biome_effects");
    }

    @Override
    public void process(Map<ResourceLocation, JsonElement> biomesJsons) {
        for (var j : biomesJsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            BiomeEffectModifier effect = BiomeEffectModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Biome Special Effect with json id " + id + "\n error: " + errorMsg))
                    .getFirst();

            addEffect(id, effect);
        }
    }

    private void addEffect(ResourceLocation pathId, BiomeEffectModifier mod) {
        var explTargets = mod.explicitTargets();
        if (!explTargets.isEmpty()) {
            for (var explicitId : explTargets) {
                effectsToApply.merge(explicitId, mod, BiomeEffectModifier::merge);
            }
        }
        //no explicit targets. use its own ID instead
        else {
            effectsToApply.merge(pathId, mod, BiomeEffectModifier::merge);
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
        if (!needsDynamicApplication) return;
        needsDynamicApplication = false;
        if (firstLogin) vanillaEffects.clear();


        Registry<Biome> biomeReg = registryAccess.registry(Registries.BIOME).get();
        addAllWaterColors(biomeReg);

        for (var v : effectsToApply.entrySet()) {
            ResourceLocation biomeId = v.getKey();
            BiomeEffectModifier modifier = v.getValue();
            var biome = biomeReg.getOptional(biomeId);
            if (biome.isPresent()) {
                var old = modifier.apply(biome.get());

                vanillaEffects.put(biomeId, old);
            }
        }
        if (!vanillaEffects.isEmpty())
            Polytone.LOGGER.info("Applied {} Custom Biome Effects Properties", vanillaEffects.size());
        //we don't clear effects to apply because we need to re apply on world reload
    }

    @Override
    public void reset() {
        this.needsDynamicApplication = true;
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            Registry<Biome> biomeReg = level.registryAccess().registry(Registries.BIOME).get();
            for (var v : vanillaEffects.entrySet()) {
                var biome = biomeReg.getOptional(v.getKey());
                biome.ifPresent(bio -> BiomeEffectModifier.applyEffects(bio, v.getValue()));
            }
            //reset all
        }
        //if we don't have a level, biomes don't exist anymore, so we don't care

        vanillaEffects.clear();

        //whatever happens, we always clear stuff to apply
        effectsToApply.clear();
    }


    //hack
    public void addAllWaterColors(Registry<Biome> biomeReg) {
        if (Polytone.sodiumOn) { //TODO:is this needed with embeddium?
            var water = Polytone.FLUID_PROPERTIES.getModifier(Fluids.WATER);
            if (water != null) {
                for (var e : biomeReg.entrySet()) {
                    var id = e.getKey().location();
                    var b = e.getValue();
                    var original = effectsToApply.get(id);
                    if (original == null || original.waterColor().isEmpty()) {
                        if (water.getTint() instanceof Colormap cl) {
                            var col = cl.getColor(b, 0, 0);
                            var dummy = BiomeEffectModifier.ofWaterColor(col);
                            addEffect(id, dummy);
                        }
                    }
                }
            }
        }

    }
}
