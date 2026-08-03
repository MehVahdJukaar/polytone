package net.mehvahdjukaar.polytone.content.item;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.companion.TexturePart;
import net.mehvahdjukaar.polytone.common.companion.TrackedTextures;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

public class ItemModifiersManager extends ContentManager<ItemModifier> {

    private final Map<Item, ItemModifier> modifiers = new HashMap<>();
    private final Map<Item, ItemModifier> vanillaProperties = new HashMap<>();


    // The item tint part (ItemModifier::getTint) from 1.21.1 isn't ported: that item-tint colormap
    // feature doesn't exist on 1.21.11 yet, so only the bar-color texture is associated here.
    private static final TexturePart<ItemModifier> BAR =
            TexturePart.suffix("_bar", ItemModifier::getBarColor);

    public ItemModifiersManager() {
        super(Spec.of("Item modifier", () -> ItemModifier.CODEC)
                .wikiPage("Item-Modifiers")
                .textureParts(BAR)
                .folders("item_modifiers", "item_properties"));
    }

    /*
    // early reload to grab the extra models we need to add.
    @Override
    public void earlyProcess(ResourceManager resourceManager) {
        var jsons = getJsonsInDirectories(resourceManager);
        for (var e : jsons.entrySet()) {
            var json = e.getValue();
            Identifier id = e.getKey();
            var partial = ItemModifier.CODEC_ONLY_MODELS.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Item Modifier with json id " + id + "\n error: " + errorMsg))
                    .getFirst();
            for (var m : partial.customModels()) {
                Polytone.addCustomModel(m.model());
            }
        }
    }*/

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        var jsons = resources.jsons();
        var textures = new TrackedTextures(resources.textures());

        Parsed.SortedMap<ItemModifier> parsedModifiers = parseAllJsons(jsons, ops);

        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers.entrySet()) {
            Identifier tintId = entry.getKey();
            Parsed<ItemModifier> result = entry.getValue();
            ItemModifier modifier = result.getResultOrPartial();

            // auto-attach a default bar colormap when a texture exists but none is declared,
            // then fill inline colormaps from the scanned textures
            if (!contentTexture.adoptable(textures, tintId, modifier).isEmpty()) {
                modifier = modifier.merge(ItemModifier.ofBarColor(Colormap.createDamage()));
            }
            contentTexture.fill(textures, tintId, modifier, true);

            if (result.isEnabled()) addModifier(tintId, modifier);
        }

        // creates orphaned texture colormaps & properties
        for (var orphan : contentTexture.orphans(textures, parsedModifiers.keySet())) {
            ItemModifier modifier = ItemModifier.ofBarColor(Colormap.createDamage());
            contentTexture.fill(textures, orphan.stemId(), modifier, true);
            addModifier(orphan.stemId(), modifier);
        }
    }

    private void addModifier(Identifier id, ItemModifier mod) {
        for (var holder : mod.targets().compute(id, BuiltInRegistries.ITEM)) {
            Item i = holder.value();
            modifiers.merge(i, mod, ItemModifier::merge);

          //  Polytone.ITEM_MODELS.addModelFromModifier(i, mod.customModels());
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
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
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
