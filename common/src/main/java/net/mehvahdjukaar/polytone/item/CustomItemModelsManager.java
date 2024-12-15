package net.mehvahdjukaar.polytone.item;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ModelResHelper;
import net.mehvahdjukaar.polytone.utils.PartialReloader;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CustomItemModelsManager extends PartialReloader<Object> {


    private final Map<Item, ItemModelOverrideList> itemModels = new Object2ObjectOpenHashMap<>();
    private final Set<ModelResourceLocation> extraModels = new HashSet<>();
    private final Set<AutoGeneratedModel> autoGeneratedModels = new HashSet<>();

    private final Map<Item, List<ItemModelOverride>> itemModifiersModels = new HashMap<>();

    public CustomItemModelsManager() {
        super("custom_item_models");

    }

    @Override
    protected Object prepare(ResourceManager resourceManager) {
        return new Object();
    }

    @Override
    protected void reset() {

    }

    @Override
    protected void process(Object obj, DynamicOps<JsonElement> ops) {

    }

    public void earlyProcess(ResourceManager resourceManager) {
        this.itemModels.clear();
        this.extraModels.clear();
        this.autoGeneratedModels.clear();
        // we must early load item modifiers aswell here...
        //TOD:
        StopWatch stopWatch = StopWatch.createStarted();

        // load item modifiers ones
        for (var l : this.itemModifiersModels.entrySet()) {
            var target = l.getKey();
            var overrides = l.getValue();
            this.itemModels.computeIfAbsent(target, a -> new ItemModelOverrideList())
                    .addAll(overrides);
            this.extraModels.addAll(overrides.stream().map(ItemModelOverride::model).toList());
        }
        this.itemModifiersModels.clear();

        var jsons = this.getJsonsInDirectories(resourceManager);
        checkConditions(jsons);

        for (var v : jsons.entrySet()) {
            JsonElement json = v.getValue();
            ResourceLocation location = v.getKey();

            StandaloneItemModelOverride modelOverride = StandaloneItemModelOverride.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Custom Item Model with json id " + location + "\n error: " + errorMsg))
                    .getFirst();

            if (modelOverride != null) {
                // merge
                // load model

                if (modelOverride.isAutoModel()) {
                    AutoGeneratedModel generated = AutoGeneratedModel.ofCIM(location);
                    this.autoGeneratedModels.add(generated);
                    modelOverride.setModel(ModelResHelper.of(generated.modelLocation()));
                } else this.extraModels.add(modelOverride.model());

                this.itemModels.computeIfAbsent(modelOverride.getTarget(), a -> new ItemModelOverrideList())
                        .add(modelOverride);
            }
        }

        Polytone.LOGGER.info("Loaded {} Custom Item Models jsons in {}", jsons.size(), stopWatch);
    }

    @Override
    protected void apply() {
        super.apply();
        int allModels = 0;
        for (var list : this.itemModels.values()) {
            allModels += list.size();
        }
        Polytone.LOGGER.info("Loaded {} Custom Item Models for {} items", allModels, this.itemModels.size());
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
        for (var list : this.itemModels.values()) {
            list.populateModels(access);
        }
    }

    @Nullable
    public BakedModel getOverride(ItemStack itemStack, @Nullable Level level, @Nullable LivingEntity entity, int seed) {
        if (level == null) return null;
        ItemModelOverrideList list = this.itemModels.get(itemStack.getItem());
        if (list == null) return null;
        return list.getModel(itemStack, level, entity, seed);
    }

    public Collection<ModelResourceLocation> getExtraModelsToLoad() {
        return extraModels;
    }

    public Map<ResourceLocation, BlockModel> createAutoGeneratedModels() {
        Map<ResourceLocation, BlockModel> models = new HashMap<>();
        for (AutoGeneratedModel model : this.autoGeneratedModels) {

            String texturePath = model.texturePath().toString();
            BlockModel unbakedModel = BlockModel.fromString(
                    String.format("""
                            {
                              "parent": "item/generated",
                              "textures": {
                                "layer0": "%s"
                              }
                            }
                            """, texturePath));

            models.put(model.modelLocation(), unbakedModel);
        }
        return models;
    }

    public void addModelFromModifier(Item item, List<ItemModelOverride> itemModelOverrides) {
        itemModifiersModels.computeIfAbsent(item, a -> new ArrayList<>()).addAll(itemModelOverrides);
    }
}
