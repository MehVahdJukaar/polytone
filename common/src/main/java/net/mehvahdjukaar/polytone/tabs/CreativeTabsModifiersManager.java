package net.mehvahdjukaar.polytone.tabs;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.utils.ItemToTabEvent;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import java.util.HashMap;
import java.util.Map;

public class CreativeTabsModifiersManager extends JsonPartialReloader {

    private final Map<ResourceKey<CreativeModeTab>, CreativeTabModifier> modifiers = new HashMap<>();


    @Override
    protected void reset() {
        modifiers.clear();
    }

    @Override
    protected void process(Map<ResourceLocation, JsonElement> obj) {

    }

    public void modifyTabs(ItemToTabEvent event) {
        for (var entry : modifiers.entrySet()) {
            var modifier = entry.getValue();
            modifier.apply(entry.getKey(), event);
        }


    }
}
