package net.mehvahdjukaar.polytone.item;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ItemModifiersManager extends JsonImgPartialReloader {

    private final Map<Item, ItemModifier> modifiers = new HashMap<>();
    private final Map<Item, ItemModifier> vanillaProperties = new HashMap<>();


    public ItemModifiersManager() {
        super("item_modifiers", "item_properties");
    }

    @Override
    public void process(Resources resources) {
        var jsons = resources.jsons();
        var textures = resources.textures();

        Set<ResourceLocation> usedTextures = new HashSet<>();

        Map<ResourceLocation, ItemModifier> parsedModifiers = new HashMap<>();

        for (var j : jsons.entrySet()) {
            JsonElement json = j.getValue();
            ResourceLocation id = j.getKey();

            ItemModifier modifier = ItemModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Item Modifier with json id {} - error: {}", id, errorMsg))
                    .getFirst();

            parsedModifiers.put(id, modifier);
        }

        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers.entrySet()) {
            ResourceLocation tintId = entry.getKey();
            ItemModifier modifier = entry.getValue();

            if (!modifier.hasTint() && textures.containsKey(tintId)) {
                //if this map doesn't have a colormap defined, we set it to the default impl IF there's a texture it can use
                modifier = modifier.merge(ItemModifier.ofItemColor(Colormap.defTriangle()));
            }
            ResourceLocation barId = tintId.withSuffix("_bar");
            if (!modifier.hasBarColor() && textures.containsKey(barId)) {
                //if this map doesn't have a bar colormap defined, we set it to the default impl IF there's a texture it can use
                modifier = modifier.merge(ItemModifier.ofBarColor(Colormap.damage()));
            }

            //fill inline colormaps colormapTextures
            ColormapsManager.tryAcceptingTexture(textures, tintId, modifier.getTint(), usedTextures, true);

            ColormapsManager.tryAcceptingTexture(textures, barId, modifier.barColor(), usedTextures, true);

            addModifier(tintId, modifier);
        }

        // creates orphaned texture colormaps & properties
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            ResourceLocation id = t.getKey();
            if(id.getPath().endsWith("_bar")){
                Colormap defaultColormap = Colormap.damage();
                ColormapsManager.tryAcceptingTexture(textures, id, defaultColormap, usedTextures, true);
                addModifier(id, ItemModifier.ofBarColor(defaultColormap));
            }else {
                Colormap defaultColormap = Colormap.defTriangle();
                ColormapsManager.tryAcceptingTexture(textures, id, defaultColormap, usedTextures, true);
                addModifier(id, ItemModifier.ofItemColor(defaultColormap));
            }
        }
    }

    private void addModifier(ResourceLocation id, ItemModifier mod) {
        for (Item item : mod.getTargets(id, BuiltInRegistries.ITEM)) {
            modifiers.merge(item, mod, ItemModifier::merge);
        }
    }

    @Override
    protected void reset() {
        for (var e : vanillaProperties.entrySet()) {
            e.getValue().apply(e.getKey());
            ((IPolytoneItem) e.getKey()).polytone$setModifier(null);
        }
        modifiers.clear();
    }

    @Override
    protected void apply() {
        for (var e : modifiers.entrySet()) {
            Item target = e.getKey();

            ItemModifier value = e.getValue();
            vanillaProperties.put(target, value.apply(target));

            if (value.shouldAttachToItem()) {
                ((IPolytoneItem) e.getKey()).polytone$setModifier(value);
            }
        }
        if (!vanillaProperties.isEmpty()) {
            Polytone.LOGGER.info("Applied {} Custom Item Properties", vanillaProperties.size());
        }
        //clear as we don't need the anymore
        modifiers.clear();

    }
}
