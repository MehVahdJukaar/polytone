package net.mehvahdjukaar.polytone.item;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ItemModelOverrideManager extends JsonImgPartialReloader {


    private final Map<Item, ItemModelOverrideList> itemModels = new Object2ObjectOpenHashMap<>();
    private final Set<ResourceLocation> extraModels = new HashSet<>();

    public ItemModelOverrideManager() {
        super("custom_item_models");

    }

    @Override
    protected void reset() {
        this.init = false;
        this.itemModels.clear();
        this.extraModels.clear();
    }

    @Override
    protected void process(Resources resources, DynamicOps<JsonElement> ops) {
        for (var v : resources.jsons().entrySet()) {
            JsonElement json = v.getValue();
            ResourceLocation location = v.getKey();
            ItemModelOverrideList modelOverride = ItemModelOverrideList.CODEC.decode(ops, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Custom Item Model with json id " + location + "\n error: " + errorMsg))
                    .getFirst();

            if (modelOverride != null) {
                // merge
                // load model
                extraModels.addAll(modelOverride.getAllModelPaths());
                this.itemModels.merge(modelOverride.getTarget(), modelOverride, ItemModelOverrideList::merge);
            }
        }
    }

    private boolean init = false;

    private void init() {
        for (ItemModelOverrideList list : this.itemModels.values()) {
            list.populateModels();
        }
    }

    @Nullable
    public BakedModel getOverride(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        if (!init) {
            this.init();
            init = true;
        }
        ItemModelOverrideList list = this.itemModels.get(itemStack.getItem());
        if (list == null) return null;
        return list.getModel(itemStack, level, entity, seed);
    }

    public Set<ResourceLocation> getExtraModels() {
        return extraModels;
    }

}
