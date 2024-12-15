package net.mehvahdjukaar.polytone.tabs;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.CsvUtils;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.mehvahdjukaar.polytone.utils.PartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import java.util.*;

public class CreativeTabsModifiersManager extends PartialReloader<CreativeTabsModifiersManager.Resources> {

    private final MapRegistry<CreativeModeTab> customTabs = new MapRegistry<>("Custom Creative Tabs");

    private final Map<ResourceKey<CreativeModeTab>, CreativeTabModifier> modifiers = new HashMap<>();
    private final Set<ResourceKey<CreativeModeTab>> needsRefresh = new HashSet<>();

    private final Map<ResourceKey<CreativeModeTab>, CreativeTabModifier> vanillaTabs = new HashMap<>();

    private final Map<ResourceLocation, JsonElement> lazyJsons = new HashMap<>();

    public CreativeTabsModifiersManager() {
        super("creative_tab_modifiers");
    }


    @Override
    public Resources prepare(ResourceManager resourceManager) {
        var jsons = getJsonsInDirectories(resourceManager);
        this.checkConditions(jsons);

        var types = CsvUtils.parseCsv(resourceManager, "creative_tabs");

        return new Resources(jsons, types);
    }

    @Override
    protected void reset() {
        customTabs.clear();
        for (var e : vanillaTabs.entrySet()) {
            e.getValue().applyAttributes(e.getKey());
        }
        vanillaTabs.clear();
        needsRefresh.addAll(modifiers.keySet());
        modifiers.clear();
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        for (var id : customTabs.keySet()) {
            PlatStuff.unregisterDynamic(BuiltInRegistries.CREATIVE_MODE_TAB, id);
            if (logOff) PlatStuff.sortTabs();
        }
    }

    @Override
    protected void process(Resources resources, DynamicOps<JsonElement> ops) {
        for (var e : resources.extraTabs.entrySet()) {
            for (var s : e.getValue()) {
                ResourceLocation id = e.getKey().withPath(s);
                ResourceKey<CreativeModeTab> key = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id);
                if (!customTabs.containsKey(id) && !BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(key)) {
                    CreativeModeTab tab = PlatStuff.createCreativeTab(id);
                    customTabs.register(id, tab);
                } else {
                    Polytone.LOGGER.error("Creative Tab with id {} already exists! Ignoring.", id);
                }
            }
        }

        for (var e : customTabs.getEntries()) {
            PlatStuff.registerDynamic(BuiltInRegistries.CREATIVE_MODE_TAB, e.getKey(), e.getValue());
        }

        if (!customTabs.isEmpty()) {
            Polytone.LOGGER.info("Registered {} custom Creative Tabs from Resource Packs: {}", customTabs.size(), customTabs + ". Remember to add items to them!");
            PlatStuff.sortTabs();
        }

        lazyJsons.clear();
        lazyJsons.putAll(resources.tabsModifiers);

        //else apply as soon as we load a level
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
        var ops = RegistryOps.create(JsonOps.INSTANCE, access);
        for (var j : lazyJsons.entrySet()) {

            JsonElement json = j.getValue();
            ResourceLocation id = j.getKey();

            CreativeTabModifier modifier = CreativeTabModifier.CODEC.decode(ops, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Creative Mode Tab Modifier with json id " + id + " - error: " + errorMsg))
                    .getFirst();

            addModifier(id, modifier);
        }
        if (!modifiers.isEmpty()) {
            needsRefresh.addAll(modifiers.keySet());
        }
        apply();
    }

    @Override
    protected void apply() {
        if (Minecraft.getInstance().level != null && !needsRefresh.isEmpty()) {
            CreativeModeTabs.CACHED_PARAMETERS = null;
            //forces reload on next open screen
            needsRefresh.clear();
        }
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
            RegistryAccess access = PlatStuff.hackyGetRegistryAccess();
            if (access != null) {
                CreativeTabModifier v = mod.applyItemsAndAttributes(event, access);
                //dont add custom tabs here!
                if (!customTabs.containsKey(tab.location())) vanillaTabs.put(tab, v);
            }
        }
    }


    public record Resources(Map<ResourceLocation, JsonElement> tabsModifiers,
                            Map<ResourceLocation, List<String>> extraTabs) {
    }

}
