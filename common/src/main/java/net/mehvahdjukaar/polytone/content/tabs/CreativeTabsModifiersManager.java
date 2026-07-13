package net.mehvahdjukaar.polytone.content.tabs;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.polytone.utils.*;
import net.minecraft.client.Minecraft;
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

public class CreativeTabsModifiersManager extends ContentManager<CreativeTabModifier, CreativeTabsModifiersManager.Resources> {

    private final MapRegistry<CreativeModeTab> customTabs = new MapRegistry<>("Custom Creative Tabs");

    private final Map<ResourceKey<CreativeModeTab>, CreativeTabModifier> modifiers = new HashMap<>();
    private final Set<ResourceKey<CreativeModeTab>> needsRefresh = new HashSet<>();

    private final Map<ResourceKey<CreativeModeTab>, CreativeTabModifier> vanillaTabs = new HashMap<>();

    public CreativeTabsModifiersManager() {
        super("Creative tab modifier", () -> SchemaCodec.wrap(CreativeTabModifier.CODEC), "creative_tab_modifiers");
    }


    @Override
    public Resources prepare(ResourceManager resourceManager) {
        var jsons = getJsonsInDirectories(resourceManager);

        var types = CsvUtils.parseCsv(resourceManager, "creative_tabs");

        return new Resources(ImmutableMap.copyOf(jsons), ImmutableMap.copyOf(types));
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        for (var id : customTabs.keySet()) {
            PlatStuff.unregisterDynamic(BuiltInRegistries.CREATIVE_MODE_TAB, id);
            if (logOff) {
                Minecraft.getInstance().tell(PlatStuff::sortTabs);
            }
        }
        customTabs.clear();
        for (var e : vanillaTabs.entrySet()) {
            e.getValue().applyAttributes(e.getKey());
        }
        vanillaTabs.clear();
        needsRefresh.addAll(modifiers.keySet());
        modifiers.clear();
    }

    @Override
    protected void parseWithLevel(Resources resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
        for (var e : resources.extraTabs.entrySet()) {
            for (var str : e.getValue()) {
                ResourceLocation id = e.getKey().withPath(str);
                registerNewTab(id);
            }
        }
        for (var e : Parsed.batchParseOnlyEnabled(resources.tabsModifiers, CreativeTabModifier.CODEC,
                ops, "creative tab modifier")) {
            ResourceLocation id = e.getKey();
            CreativeTabModifier mod = e.getValue();
            if (mod.registerTab()) {
                registerNewTab(id);
            }
            addModifier(e.getKey(), e.getValue());
        }


        if (!customTabs.isEmpty()) {
            Polytone.LOGGER.info("Registered {} custom Creative Tabs from Resource Packs: {}", customTabs.size(), customTabs + ". Remember to add items to them!");
            Minecraft.getInstance().tell(PlatStuff::sortTabs);
        }

    }

    private void registerNewTab(ResourceLocation id) {
        ResourceKey<CreativeModeTab> key = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id);
        if (!customTabs.containsKey(id) && !BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(key)) {
            CreativeModeTab tab = PlatStuff.createCreativeTab(id);
            customTabs.register(id, tab);
            PlatStuff.registerDynamic(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab);
        } else {
            Polytone.LOGGER.error("Creative Tab with id {} already exists! Ignoring.", id);
        }
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {

        if (!modifiers.isEmpty()) {
            needsRefresh.addAll(modifiers.keySet());
        }
        if (!needsRefresh.isEmpty()) {
            CreativeModeTabs.CACHED_PARAMETERS = null;
            //forces reload on next open screen
            needsRefresh.clear();
        }
    }

    private void addModifier(ResourceLocation fileId, CreativeTabModifier mod) {
        Targets targets = mod.targets();
        if (mod.registerTab()) {
            targets = Targets.ofIds(fileId);
        }
        for (var tab : targets.compute(fileId, BuiltInRegistries.CREATIVE_MODE_TAB.asLookup())) {
            ResourceKey<CreativeModeTab> key = tab.unwrapKey().get();
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
                //don't add custom tabs here!
                if (!customTabs.containsKey(tab.location())) vanillaTabs.put(tab, v);
            }
        }
    }

    public boolean isDynamicTab(ResourceLocation entryId) {
        return customTabs.containsKey(entryId);
    }


    public record Resources(Map<ResourceLocation, JsonElement> tabsModifiers,
                            Map<ResourceLocation, List<String>> extraTabs) {
    }

}
