
package net.mehvahdjukaar.polytone.content.dimension;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.reloader.JsonImgPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.HashMap;
import java.util.Map;

public class DimensionEffectsManager extends JsonImgPartialReloader {

    private final Map<Identifier, DimensionEffectsModifier> effectsToApply = new HashMap<>();

    private final Map<Identifier, DimensionEffectsModifier> alteredVanillaEffects = new HashMap<>();

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
        for (var h : mod.targets().compute(fileId, registryAccess)) {
            effectsToApply.merge(h.unwrapKey().get().identifier(), mod, DimensionEffectsModifier::merge);
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {

        //reset vanilla
        var entries = alteredVanillaEffects.entrySet().iterator();
        while (entries.hasNext()) {
            var v = entries.next();
            Identifier key = v.getKey();
            var dimensionKey = ResourceKey.create(Registries.DIMENSION_TYPE, key);
            var dimType = access.lookupOrThrow(Registries.DIMENSION_TYPE).get(dimensionKey);
            if (dimType.isPresent()) {
                DimensionEffectsModifier oldMod = v.getValue();
                oldMod.apply(dimType.get().value());
                entries.remove();
            }
        }

        //apply to current dimension
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            onDimensionChanged(level.dimensionTypeRegistration(), access);
        }
    }

    public void onDimensionChanged(Holder<DimensionType> currentDimHolder, HolderLookup.Provider access) {
        DimensionType currentDim = currentDimHolder.value();
        Identifier currentDimId = currentDimHolder.unwrapKey().get().identifier();

        for (var v : effectsToApply.entrySet()) {
            Identifier modId = v.getKey();
            DimensionEffectsModifier modifier = v.getValue();
            var targets = modifier.targets().compute(modId, access);
            if (!targets.contains(currentDimHolder)) continue;

            DimensionEffectsModifier old = modifier.apply(currentDim);

            alteredVanillaEffects.put(currentDimId, old);

            Polytone.LOGGER.info("Applied Custom Dimension Effects Modifier '{}' to dimension '{}'", modId, currentDimHolder);
        }
        //we don't clear effects to apply because we need to re apply on world reload
    }


    public void addConvertedBlockProperties(Map<Identifier, Parsed<DimensionEffectsModifier>> converted) {
        extraMods.clear();
        extraMods.putAll(converted);
    }

    public void addPostLayers(EnvironmentAttributeSystem.Builder builder, Level level) {

    }

    public boolean hasModifiedAttributes() {
        return effectsToApply.values().stream().anyMatch(
                m -> !m.attributeModifications().isEmpty());
    }
}
