package net.mehvahdjukaar.polytone.compat.nautilus;

import net.mehvahdjukaar.nautilus.NautilusStudioApi;
import net.mehvahdjukaar.nautilus.env.ClientEnvironment;
import net.mehvahdjukaar.nautilus.env.EnvironmentResolver;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.attributes.EnvironmentAttributeMapMod;
import net.mehvahdjukaar.polytone.common.attributes.IExtendedEntry;
import net.mehvahdjukaar.polytone.content.biome.BiomeEffectModifier;
import net.mehvahdjukaar.polytone.content.dimension.DimensionEffectsModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// Polytone's attribute layers live on the client only: the server never hears about them, so the editor's
// environment-attribute view can't find them by folding pack data. This tells it what we installed, which
// is what turns "the live value doesn't match the fold" into "this biome_modifier is overriding it".
final class NautilusEnvironment implements ClientEnvironment.Contributor {

    static void register() {
        NautilusStudioApi.register(new NautilusEnvironment());
    }

    @Override
    public List<ClientEnvironment.Row> describe(EnvironmentAttribute<?> attribute) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        HolderLookup.Provider registries = level.registryAccess();

        List<ClientEnvironment.Row> rows = new ArrayList<>();
        for (var e : Polytone.DIMENSION_MODIFIERS.modifiersById().entrySet()) {
            var mods = e.getValue().attributeModifications();
            String source = "dimension_modifiers/" + e.getKey().getPath();
            add(rows, attribute, registries, source, "dimension", mods.baseMod());
            add(rows, attribute, registries, source, "dimension, while raining", mods.rainMod());
            add(rows, attribute, registries, source, "dimension, while thundering", mods.thunderMod());
            add(rows, attribute, registries, source, "after the vanilla fold", mods.postProcess());
        }
        for (var e : Polytone.BIOME_MODIFIERS.modifiersByBiome().entrySet()) {
            BiomeEffectModifier.BiomeEnvAttributeModifications mods = e.getValue().attributeModifications();
            String source = "biome " + biomeName(registries, e.getKey());
            add(rows, attribute, registries, source, "biome", mods.baseMod());
            add(rows, attribute, registries, source, "after the vanilla fold", mods.postProcess());
        }
        return rows;
    }

    private static void add(List<ClientEnvironment.Row> rows, EnvironmentAttribute<?> attribute,
                            HolderLookup.Provider registries, String source, String stage,
                            EnvironmentAttributeMapMod mod) {
        if (mod.removes(attribute)) {
            rows.add(new ClientEnvironment.Row(source, stage, "remove", null, null,
                    "Drops the entry this map had, so the layer below shows through."));
            return;
        }
        EnvironmentAttributeMap.Entry<?, ?> entry = mod.getEntry(attribute);
        if (entry == null) return;

        EnvironmentResolver.EntryText text = EnvironmentResolver.describeEntry(attribute, entry, registries);
        rows.add(new ClientEnvironment.Row(source, stage, text.operation(), text.argument(), text.argb(),
                dynamicNote(entry)));
    }

    // A colormap- or expression-backed argument is re-read every frame, so the number in the row is only
    // what it happened to be at this sample. Worth saying, or the row reads like a constant.
    private static @Nullable String dynamicNote(EnvironmentAttributeMap.Entry<?, ?> entry) {
        IExtendedEntry<?> ext = (IExtendedEntry<?>) (Object) entry;
        if (ext.polytone$getArgumentSupplier() == null) return null;
        return ext.polytone$shouldBlend()
                ? "Computed per frame from a colormap or expression, blended across biome borders."
                : "Computed per frame from a colormap or expression, sampled at the camera.";
    }

    private static String biomeName(HolderLookup.Provider registries, Biome biome) {
        return registries.lookup(Registries.BIOME)
                .flatMap(lookup -> lookup.listElements().filter(h -> h.value() == biome).findFirst())
                .map(h -> h.key().identifier().toString())
                .orElse("(unnamed)");
    }
}
