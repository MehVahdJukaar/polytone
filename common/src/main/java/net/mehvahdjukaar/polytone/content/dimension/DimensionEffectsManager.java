
package net.mehvahdjukaar.polytone.content.dimension;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.misc.Parsed;
import net.mehvahdjukaar.polytone.misc.reloader.JsonImgPartialReloader;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DimensionEffectsManager extends JsonImgPartialReloader {

    private final Map<Identifier, DimensionEffectsModifier> effectsToApply = new HashMap<>();

    private final Map<Identifier, DimensionEffectsModifier> vanillaEffects = new HashMap<>();

    private final Map<Identifier, Parsed<DimensionEffectsModifier>> extraMods = new HashMap<>();

    public DimensionEffectsManager() {
        super("dimension_modifiers", "dimension_effects");
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        //Dimensions are NOT reloaded with world load. we need to reset vanilla stuff once we have a level
        //whatever happens, we always clear stuff to apply
        effectsToApply.clear();
        extraMods.clear();
    }

    @Override
    protected void parseWithLevel(Resources resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        var jsons = resources.jsons();

        Parsed.SortedMap<DimensionEffectsModifier> parsedModifiers =
                Parsed.batchParseAlways(jsons, DimensionEffectsModifier.CODEC, ops, "dimension modifier");
        parsedModifiers.putAll(extraMods);

        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers) {
            Identifier id = entry.getKey();
            Parsed<DimensionEffectsModifier> parsed = entry.getValue();
            DimensionEffectsModifier modifier = parsed.getResultOrPartial();

            if (parsed.isEnabled()) {
                addModifier(id, modifier, access);
            }
        }
        //TODO: maybe add back texture stuff
    }

    private void addModifier(Identifier fileId, DimensionEffectsModifier mod, HolderLookup.Provider registryAccess) {
        for (var h : mod.targets().getTargets(fileId, registryAccess)) {
            effectsToApply.merge(h.unwrapKey().get().identifier(), mod, DimensionEffectsModifier::merge);
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider registryAccess, boolean isLogIn) {
        for (var v : vanillaEffects.entrySet()) {
            v.getValue().apply(v.getKey());
        }

        var dimReg = registryAccess.lookupOrThrow(Registries.DIMENSION_TYPE);

        for (var v : effectsToApply.entrySet()) {
            Identifier dimensionId = v.getKey();
            var dimensionKey = ResourceKey.create(Registries.DIMENSION_TYPE, dimensionId);
            DimensionEffectsModifier modifier = v.getValue();
            var old = modifier.applyInplace(dimensionId);

            vanillaEffects.put(dimensionId, old);

            DimensionType dim = dimReg.get(dimensionKey).get().value();

        }
        if (!vanillaEffects.isEmpty())
            Polytone.LOGGER.info("Applied {} Dimension Modifiers", vanillaEffects.size());
        //we don't clear effects to apply because we need to re apply on world reload

    }

    public void applyOnDimensionChanged(){

    }

    public void addConvertedBlockProperties(Map<Identifier, Parsed<DimensionEffectsModifier>> converted) {
        extraMods.clear();
        extraMods.putAll(converted);
    }

}
