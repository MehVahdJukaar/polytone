package net.mehvahdjukaar.polytone.item;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ItemModelOverrideList {

    private final Map<Key, BakedModel> overrides = new Object2ObjectOpenHashMap<>();
    private final Set<DataComponentType<?>> keys = new HashSet<>();
    private final List<ItemModelOverride> entries = new ArrayList<>();
    private boolean populated = false;

    // initialize
    private void populateModels() {
        this.keys.clear();
        this.overrides.clear();
        for (ItemModelOverride entry : this.entries) {
            this.overrides.put(new Key(entry.components().stream()
                    .toArray(TypedDataComponent<?>[]::new)),  PlatStuff.getBakedModel(entry.model()));
        }

        for (var map : this.overrides.keySet()) {
            keys.addAll(Arrays.stream(map.types).map(TypedDataComponent::type).toList());
        }

        this.populated = true;
        this.entries.clear();
    }

    public void addAll(Collection<ItemModelOverride> itemModelOverrides) {
        this.entries.addAll(itemModelOverrides);
    }

    public void add(ItemModelOverride itemModelOverride) {
        this.entries.add(itemModelOverride);
    }


    @Nullable
    public BakedModel getModel(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        if (!populated) this.populateModels();
        DataComponentMap components = stack.getComponents();
        if (components.isEmpty()) return null;

        TypedDataComponent<?>[] key = new TypedDataComponent<?>[this.keys.size()];
        int i = 0;
        for (DataComponentType<?> type : this.keys) {
            key[i++] = components.getTyped(type);
        }
        return this.overrides.get(new Key(key));
    }

    public int size() {
        return entries.size();
    }


    public record Key(TypedDataComponent<?>... types) {

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
