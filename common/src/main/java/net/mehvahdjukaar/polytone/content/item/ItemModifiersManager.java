package net.mehvahdjukaar.polytone.content.item;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.companion.TexturePart;
import net.mehvahdjukaar.polytone.companion.TrackedTextures;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.mehvahdjukaar.polytone.utils.Parsed;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

public class ItemModifiersManager extends JsonImgPartialReloader<ItemModifier> {

    private static final String BAR_SUFFIX = "_bar";

    private final Map<Item, ItemModifier> modifiers = new HashMap<>();
    private final Map<Item, ItemModifier> vanillaProperties = new HashMap<>();


    private static final TexturePart<ItemModifier> TINT = TexturePart.plain("tint", ItemModifier::getTint);
    private static final TexturePart<ItemModifier> BAR = TexturePart.suffix(BAR_SUFFIX, ItemModifier::getBarColor);

    public ItemModifiersManager() {
        super(Spec.of("Item modifier", () -> ItemModifier.CODEC)
                .wikiPage("Item-Modifiers")
                .textureParts(TINT, BAR)
                .folders("item_modifiers", "item_properties"));
    }

    private static ItemModifier defaultFor(TexturePart<ItemModifier> part) {
        return part == BAR ? ItemModifier.ofBarColor(Colormap.createDamage())
                : ItemModifier.ofItemColor(Colormap.createDefTriangle());
    }

    // early reload to grab the extra models we need to add.
    @Override
    public void earlyProcess(ResourceManager resourceManager) {
        var jsons = getJsonsInDirectories(resourceManager);
        for (var e : jsons.entrySet()) {
            var json = e.getValue();
            ResourceLocation id = e.getKey();
            var partial = ItemModifier.CODEC_ONLY_MODELS.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Item Modifier with json id " + id + "\n error: " + errorMsg))
                    .getFirst();
            for (var m : partial.customModels()) {
                Polytone.addCustomModel(m.model());
            }
        }
    }

    @Override
    protected void parseWithLevel(Resources resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
        var jsons = resources.jsons();
        var textures = new TrackedTextures(resources.textures());

        Parsed.SortedMap<ItemModifier> parsedModifiers =
                Parsed.batchParseAlways(jsons, ItemModifier.CODEC, ops, "item modifier");

        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers.entrySet()) {
            ResourceLocation id = entry.getKey();
            Parsed<ItemModifier> result = entry.getValue();
            ItemModifier modifier = result.getResultOrPartial();

            // auto-attach defaults for lone textures, then fill inline colormaps from the scanned ones
            for (var part : contentTexture.adoptable(textures, id, modifier).keySet()) {
                modifier = modifier.merge(defaultFor(part));
            }
            contentTexture.fill(textures, id, modifier, true);

            if (result.isEnabled()) addModifier(id, modifier);
        }

        // creates orphaned texture colormaps & properties. A lone <name>_bar.png colors the
        // durability bar of item <name> (the suffix is convention, not part of the target id)
        for (var orphan : contentTexture.orphans(textures, parsedModifiers.keySet())) {
            ItemModifier modifier = null;
            for (var part : orphan.parts().keySet()) {
                ItemModifier d = defaultFor(part);
                modifier = modifier == null ? d : modifier.merge(d);
            }
            contentTexture.fill(textures, orphan.stemId(), modifier, true);
            addModifier(orphan.stemId(), modifier);
        }
    }

    private void addModifier(ResourceLocation id, ItemModifier mod) {
        for (var item : mod.targets().compute(id, BuiltInRegistries.ITEM.asLookup())) {
            var i = item.value();
            modifiers.merge(i, mod, ItemModifier::merge);

            Polytone.ITEM_MODELS.addModelFromModifier(i, mod.customModels());
            mod.coloredLight().ifPresent(l -> Polytone.COLORED_LIGHTS.addItemLight(i, l));
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
