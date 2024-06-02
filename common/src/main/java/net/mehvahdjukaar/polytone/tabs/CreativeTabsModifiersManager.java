package net.mehvahdjukaar.polytone.tabs;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CreativeTabsModifiersManager extends JsonPartialReloader {

    private final Multimap<ResourceKey<CreativeModeTab>, CreativeTabModifier> modifiers = HashMultimap.create();
    private final Set<ResourceKey<CreativeModeTab>> needsRefresh = new HashSet<>();


    public CreativeTabsModifiersManager() {
        super("creative_tab_modifiers");
    }

    @Override
    protected void reset() {
        needsRefresh.addAll(modifiers.keys());
        modifiers.clear();
    }

    @Override
    protected void process(Map<ResourceLocation, JsonElement> jsons) {
        for (var j : jsons.entrySet()) {
            JsonElement json = j.getValue();
            ResourceLocation id = j.getKey();

            CreativeTabModifier modifier = CreativeTabModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Creative Mode Tab Modifier with json id {} - error: {}", id, errorMsg))
                    .getFirst();

            addModifier(id, modifier);
        }
        if (!modifiers.isEmpty()) {
            needsRefresh.addAll(modifiers.keys());
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
            modifiers.put(key, mod);
            PlatStuff.addTabEventForTab(key);
        }
    }

    public void modifyTab(ItemToTabEvent event) {
        var tab = event.getTab();
        for (var modifier : modifiers.get(tab)) {
            modifier.apply(event);
        }
    }
}
