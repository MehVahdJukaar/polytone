package net.mehvahdjukaar.polytone.item;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.mehvahdjukaar.polytone.utils.Parsed;
import net.mehvahdjukaar.polytone.utils.Utils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
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

    @Override
    protected void parseWithLevel(Resources resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
        var jsons = resources.jsons();
        var textures = new HashMap<>(resources.textures());

        Set<ResourceLocation> usedTextures = new HashSet<>();

        Map<ResourceLocation, Parsed<ItemModifier>> parsedModifiers = Utils.sortedMap();

        for (var j : jsons.entrySet()) {
            JsonElement json = j.getValue();
            ResourceLocation id = j.getKey();

            var modifier = Parsed.parseFull(ItemModifier.CODEC, json, ops, id, "item modifier");

            parsedModifiers.put(id, modifier);

        }

        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers.entrySet()) {
            ResourceLocation tintId = entry.getKey();
            Parsed<ItemModifier> result = entry.getValue();
            ItemModifier modifier = result.getResultOrPartial();

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

            if (result.isEnabled()) addModifier(tintId, modifier);
        }

        // creates orphaned texture colormaps & properties
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            ResourceLocation id = t.getKey();
            if(id.getPath().endsWith("_bar")){
                Colormap defaultColormap = Colormap.createDamage();
                ColormapsManager.tryAcceptingTexture(textures, id, defaultColormap, usedTextures, true);
                addModifier(id, ItemModifier.ofBarColor(defaultColormap));
            }else {
                Colormap defaultColormap = Colormap.createDefTriangle();
                ColormapsManager.tryAcceptingTexture(textures, id, defaultColormap, usedTextures, true);
                addModifier(id, ItemModifier.ofItemColor(defaultColormap));
            }
        }
    }

    private void addModifier(ResourceLocation id, ItemModifier mod) {
        for (var item : mod.targets().compute(id, BuiltInRegistries.ITEM)) {
            var i = item.value();
            modifiers.merge(i, mod, ItemModifier::merge);
        }
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        for (var e : vanillaProperties.entrySet()) {
            e.getValue().apply(e.getKey());
            ((IPolytoneItem) e.getKey()).polytone$setModifier(null);
        }
        modifiers.clear();
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
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
