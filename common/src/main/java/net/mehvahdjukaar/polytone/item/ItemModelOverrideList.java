package net.mehvahdjukaar.polytone.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ItemModelOverrideList {

    public static final Codec<ItemModelOverrideList> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemModelOverride.CODEC.listOf().fieldOf("overrides").forGetter(e -> e.entries),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ItemModelOverrideList::getTarget)
    ).apply(instance, ItemModelOverrideList::new));

    private final Map<Key, BakedModel> overrides = new Object2ObjectOpenHashMap<>();
    private final Set<DataComponentType<?>> keys = new HashSet<>();
    private final List<ItemModelOverride> entries;
    private final Item target;

    private ItemModelOverrideList(List<ItemModelOverride> entries, Item target) {
        this.entries = entries;
        this.target = target;
    }

    public Item getTarget() {
        return target;
    }

    // initialize
    public void populateModels() {
        keys.clear();
        overrides.clear();
        for (ItemModelOverride entry : entries) {
            overrides.put(new Key(entry.getComponents()), entry.fetchModel());
        }

        for (var map : overrides.keySet()) {
            keys.addAll(Arrays.stream(map.types).map(TypedDataComponent::type).toList());
        }
    }


    @Nullable
    public BakedModel getModel(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        DataComponentMap components = stack.getComponents();
        if (components.isEmpty()) return null;

        TypedDataComponent<?>[] key = new TypedDataComponent<?>[this.keys.size()];
        int i = 0;
        for (DataComponentType<?> type : this.keys) {
            key[i++] = components.getTyped(type);
        }
        return this.overrides.get(new Key(key));
    }

    public Set<ResourceLocation> getAllModelPaths() {
        return this.entries.stream().map(ItemModelOverride::model).collect(Collectors.toSet());
    }

    public ItemModelOverrideList merge(ItemModelOverrideList other) {
        this.entries.addAll(other.entries);
        return this;
    }


    public record ItemModelOverride(DataComponentMap components, ResourceLocation model) {

        public static final Codec<ItemModelOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                DataComponentMap.CODEC.fieldOf("components").forGetter(ItemModelOverride::components),
                ResourceLocation.CODEC.fieldOf("model").forGetter(ItemModelOverride::model)
        ).apply(instance, ItemModelOverride::new));


        private BakedModel fetchModel() {
            return PlatStuff.getBakedModel(this.model);
        }

        private TypedDataComponent<?>[] getComponents() {
            return this.components.stream().toArray(TypedDataComponent<?>[]::new);
        }

    }

    public record Key(TypedDataComponent<?> ...types) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.deepEquals(types, key.types);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(types);
        }
    }
}
