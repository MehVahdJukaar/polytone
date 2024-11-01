package net.mehvahdjukaar.polytone.item;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
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

    // early reload to grab the extra models we need to add. Ugly but needed as model manager reloads before all these
    public void earlyProcess(ResourceManager resourceManager) {
        var jsons = getJsonsInDirectories(resourceManager);
        for (var e : jsons.entrySet()) {
            var json = e.getValue();
            ResourceLocation id = e.getKey();
            var partial = ItemModifier.CODEC_PARTIAL.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Item Modifier with json id " + id + "\n error: " + errorMsg))
                    .getFirst();
            if (!partial.customModels().isEmpty()) {
                for (var item : partial.getTargets(id, BuiltInRegistries.ITEM)) {
                    Polytone.ITEM_MODELS.addModelFromModifier(item.value(), partial.customModels());
                }
            }
        }

    }

    @Override
    public void process(Resources resources, DynamicOps<JsonElement> ops) {
        var jsons = resources.jsons();
        var textures = resources.textures();

        Set<ResourceLocation> usedTextures = new HashSet<>();

        Map<ResourceLocation, ItemModifier> parsedModifiers = new HashMap<>();

        for (var j : jsons.entrySet()) {
            JsonElement json = j.getValue();
            ResourceLocation location = j.getKey();

            ItemModifier modifier = ItemModifier.CODEC.decode(ops, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Item Modifier with json id " + location + "\n error: " + errorMsg))
                    .getFirst();

            parsedModifiers.put(location, modifier);
        }

        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers.entrySet()) {
            ResourceLocation tintId = entry.getKey();
            ItemModifier modifier = entry.getValue();

            if (!modifier.hasTint() && textures.containsKey(tintId)) {
                //if this map doesn't have a colormap defined, we set it to the default impl IF there's a texture it can use
                modifier = modifier.merge(ItemModifier.ofItemColor(Colormap.createDefTriangle()));
            }
            ResourceLocation barId = tintId.withSuffix("_bar");
            if (!modifier.hasBarColor() && textures.containsKey(barId)) {
                //if this map doesn't have a bar colormap defined, we set it to the default impl IF there's a texture it can use
                modifier = modifier.merge(ItemModifier.ofBarColor(Colormap.createDamage()));
            }

            //fill inline colormaps colormapTextures
            ColormapsManager.tryAcceptingTexture(textures, tintId, modifier.getTint(), usedTextures, true);

            ColormapsManager.tryAcceptingTexture(textures, barId, modifier.getBarColor(), usedTextures, true);

            addModifier(tintId, modifier);
        }

        // creates orphaned texture colormaps & properties
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            ResourceLocation id = t.getKey();
            if (id.getPath().endsWith("_bar")) {
                Colormap defaultColormap = Colormap.createDamage();
                ColormapsManager.tryAcceptingTexture(textures, id, defaultColormap, usedTextures, true);
                addModifier(id, ItemModifier.ofBarColor(defaultColormap));
            } else {
                Colormap defaultColormap = Colormap.createDefTriangle();
                ColormapsManager.tryAcceptingTexture(textures, id, defaultColormap, usedTextures, true);
                addModifier(id, ItemModifier.ofItemColor(defaultColormap));
            }
        }
    }

    private void addModifier(ResourceLocation id, ItemModifier mod) {
        for (var item : mod.getTargets(id, BuiltInRegistries.ITEM)) {
            modifiers.merge(item.value(), mod, ItemModifier::merge);
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

            ItemModifier modifier = e.getValue();
            vanillaProperties.put(target, modifier.apply(target));

            if (modifier.shouldAttachToItem()) {
                ((IPolytoneItem) e.getKey()).polytone$setModifier(modifier);
            }

            //if (!modifier.customModels().isEmpty()) {
            //    Polytone.ITEM_MODELS.addModel(target, modifier.customModels());
            //}
        }
        if (!vanillaProperties.isEmpty()) {
            Polytone.LOGGER.info("Applied {} Custom Item Properties", vanillaProperties.size());
        }
        //clear as we don't need the anymore
        modifiers.clear();

    }
}
