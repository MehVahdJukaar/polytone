package net.mehvahdjukaar.polytone.tabs;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CreativeTabsModifiersManager extends JsonPartialReloader {

    private final Map<ResourceKey<CreativeModeTab>, CreativeTabModifier> modifiers = new HashMap<>();
    private final Set<ResourceKey<CreativeModeTab>> needsRefresh = new HashSet<>();

    private final Map<ResourceKey<CreativeModeTab>, CreativeTabModifier> vanillaTabs = new HashMap<>();


    public CreativeTabsModifiersManager() {
        super("creative_tab_modifiers");
    }

    @Override
    protected void reset() {
        for (var e : vanillaTabs.entrySet()) {
            e.getValue().applyAttributes(e.getKey());
        }
        vanillaTabs.clear();
        needsRefresh.addAll(modifiers.keySet());
        modifiers.clear();
    }

    @Override
    protected void process(Map<ResourceLocation, JsonElement> jsons) {
        for (var j : jsons.entrySet()) {
            JsonElement json = j.getValue();
            ResourceLocation id = j.getKey();

            CreativeTabModifier modifier = CreativeTabModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Creative Mode Tab Modifier with json id " + id + " - error: " + errorMsg))
                    .getFirst();

            addModifier(id, modifier);
        }
        if (!modifiers.isEmpty()) {
            needsRefresh.addAll(modifiers.keySet());
        }
    }

    @Override
    protected void apply() {
        if (!needsRefresh.isEmpty() && CreativeModeTabs.CACHED_PARAMETERS != null) {
            //this only happens if they have already been built

            for (var key : needsRefresh) {
                BuiltInRegistries.CREATIVE_MODE_TAB.getOptional(key).ifPresent(tab -> {
                    tab.buildContents(CreativeModeTabs.CACHED_PARAMETERS);
                });
            }

            //rebuild all the rest
            BuiltInRegistries.CREATIVE_MODE_TAB.stream().filter((t) -> t.getType() != CreativeModeTab.Type.CATEGORY)
                    .forEach((creativeModeTab) -> {
                        creativeModeTab.buildContents(CreativeModeTabs.CACHED_PARAMETERS);
                    });
        }
        needsRefresh.clear();
    }


    private void addModifier(ResourceLocation fileId, CreativeTabModifier mod) {
        for (ResourceLocation id : mod.getTargetsKeys(fileId)) {
            ResourceKey<CreativeModeTab> key = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id);
            modifiers.merge(key, mod, CreativeTabModifier::merge);

            PlatStuff.addTabEventForTab(key);
        }
    }

    public void modifyTab(ItemToTabEvent event) {
        var tab = event.getTab();
        var mod = modifiers.get(tab);
        if (mod != null) {
            vanillaTabs.put(tab, mod.applyItemsAndAttributes(event));
        }
    }
}
