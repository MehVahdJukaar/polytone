package net.mehvahdjukaar.polytone.item;

import net.mehvahdjukaar.polytone.utils.FrequencyOrderedCollection;
import net.mehvahdjukaar.polytone.utils.SearchTrie;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ItemModelOverrideList {

    private final PropertiesSearchTrie overrides = new PropertiesSearchTrie();
    private final List<ItemModelOverride> entries = new ArrayList<>();

    private boolean populated = false;

    // initialize
    private void populateModels() {
        // staff : name "staff", enchant "fire
        // spear : name "sprear"
        // trident : count 1
        // key: name, enchant, count
        this.overrides.clear();
        this.overrides.overridesAcceptEntries(this.entries);
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
        return null;

        /*
        TypedDataComponent<?>[] key = new TypedDataComponent<?>[this.orderedKeys.size()];
        int i = 0;
        for (DataComponentType<?> type : this.orderedKeys) {
            key[i++] = components.getTyped(type);
        }
        return this.overrides.get(new Key(key));
        */
    }

    public int size() {
        return entries.size();
    }


    public static class PropertiesSearchTrie extends SearchTrie<TypedDataComponent<?>, ResourceLocation> {

        private final List<DataComponentType<?>> orderedKeys = new ArrayList<>();

        @Override
        public void clear() {
            super.clear();
            orderedKeys.clear();
        }

        protected Collection<ResourceLocation> search(TypedDataComponent<?>... types) {
            // Initialize a list of null values with the size of orderedKeys
            List<TypedDataComponent<?>> paths = new ArrayList<>(Collections.nCopies(this.orderedKeys.size(), null));

            // Create a map for quick lookup of types by their class
            Map<DataComponentType<?>, TypedDataComponent<?>> typeMap = Arrays.stream(types)
                    .collect(Collectors.toMap(TypedDataComponent::type, t -> t));

            // Fill in the result list based on orderedKeys
            for (int i = 0; i < this.orderedKeys.size(); i++) {
                DataComponentType<?> keyType = this.orderedKeys.get(i);
                TypedDataComponent<?> component = typeMap.get(keyType);
                paths.set(i, component);
            }

            // Return the result as a collection
            return search(paths);
        }

        protected List<Object> makePath(ItemStack stack) {
            DataComponentMap components = stack.getComponents();
            if (components.isEmpty()) return Collections.emptyList();

            List<Object> key = new ArrayList<>(this.orderedKeys.size());
            for (DataComponentType<?> type : this.orderedKeys) {
                key.add(components.getTyped(type));
            }
            return key;
        }

        public void overridesAcceptEntries(List<ItemModelOverride> entries) {
            FrequencyOrderedCollection<DataComponentType<?>> keyFrequencies = new FrequencyOrderedCollection<>();
            for (ItemModelOverride entry : entries) {
                for (var component : entry.components()) {
                    keyFrequencies.add(component.type());
                }
            }
            // assumes keys with more values will be more common. actually inverse to minimize space
            this.orderedKeys.addAll(keyFrequencies.stream().toList().reversed());

            for (ItemModelOverride entry : entries) {
                List<TypedDataComponent<?>> key = new ArrayList<>(this.orderedKeys.size());
                for (DataComponentType<?> type : this.orderedKeys) {
                    key.add(entry.components().getTyped(type));
                }
                this.insert(key, entry.model());
            }

        }

    }


    public static void testTrie() {
        PropertiesSearchTrie trie = new PropertiesSearchTrie();

        // Define test cases with different combinations of key-value pairs
        List<ItemModelOverride> list = new ArrayList<>();

        // Test data with single key-value pairs
        list.add(ItemModelOverride.of(
                DataComponentMap.builder()
                        .set(DataComponents.CUSTOM_NAME, Component.literal("staff"))
                        .build(),
                ResourceLocation.tryParse("staff")
        ));

        list.add(ItemModelOverride.of(
                DataComponentMap.builder()
                        .set(DataComponents.MAX_STACK_SIZE, 1)
                        .build(),
                ResourceLocation.tryParse("shield")
        ));

        list.add(ItemModelOverride.of(
                DataComponentMap.builder()
                        .set(DataComponents.BASE_COLOR, DyeColor.BLACK)
                        .build(),
                ResourceLocation.tryParse("banner")
        ));

        // Combinations of key-value pairs
        list.add(ItemModelOverride.of(
                DataComponentMap.builder()
                        .set(DataComponents.CUSTOM_NAME, Component.literal("spear"))
                        .set(DataComponents.MAX_STACK_SIZE, 1)
                        .build(),
                ResourceLocation.tryParse("spear")
        ));

        list.add(ItemModelOverride.of(
                DataComponentMap.builder()
                        .set(DataComponents.BASE_COLOR, DyeColor.BLACK)
                        .set(DataComponents.MAX_STACK_SIZE, 2)
                        .build(),
                ResourceLocation.tryParse("flag")
        ));

        list.add(ItemModelOverride.of(
                DataComponentMap.builder()
                        .set(DataComponents.CUSTOM_NAME, Component.literal("axe"))
                        .set(DataComponents.MAX_STACK_SIZE, 3)
                        .build(),
                ResourceLocation.tryParse("axe")
        ));

        list.add(ItemModelOverride.of(
                DataComponentMap.builder()
                        .set(DataComponents.CUSTOM_NAME, Component.literal("pickaxe"))
                        .build(),
                ResourceLocation.tryParse("pickaxe")
        ));

        list.add(ItemModelOverride.of(
                DataComponentMap.builder()
                        .set(DataComponents.CUSTOM_NAME, Component.literal("gun"))
                        .build(),
                ResourceLocation.tryParse("gun")
        ));

        // Additional combinations with variations
        list.add(ItemModelOverride.of(
                DataComponentMap.builder()
                        .set(DataComponents.BASE_COLOR, DyeColor.RED)
                        .set(DataComponents.MAX_STACK_SIZE, 5)
                        .build(),
                ResourceLocation.tryParse("red_flag")
        ));

        list.add(ItemModelOverride.of(
                DataComponentMap.builder()
                        .set(DataComponents.CUSTOM_NAME, Component.literal("sword"))
                        .set(DataComponents.MAX_STACK_SIZE, 1)
                        .set(DataComponents.BASE_COLOR, DyeColor.BLUE)
                        .build(),
                ResourceLocation.tryParse("blue_sword")
        ));

        list.add(ItemModelOverride.of(
                DataComponentMap.builder()
                        .set(DataComponents.CUSTOM_NAME, Component.literal("helmet"))
                        .build(),
                ResourceLocation.tryParse("helmet")
        ));

        list.add(ItemModelOverride.of(
                DataComponentMap.builder()
                        .set(DataComponents.BASE_COLOR, DyeColor.GREEN)
                        .build(),
                ResourceLocation.tryParse("green_item")
        ));

        // Insert all test data into the trie
        trie.overridesAcceptEntries(list);

        // Print the trie structure
        System.out.println("Trie structure after insertions:");
        trie.printTrie();

        TypedDataComponent<DyeColor> red = new TypedDataComponent<>(DataComponents.BASE_COLOR, DyeColor.RED);
        TypedDataComponent<DyeColor> yellow = new TypedDataComponent<>(DataComponents.BASE_COLOR, DyeColor.YELLOW);
        TypedDataComponent<Integer> one = new TypedDataComponent<>(DataComponents.MAX_STACK_SIZE, 1);
        TypedDataComponent<Component> staff = new TypedDataComponent<>(DataComponents.CUSTOM_NAME, Component.literal("staff"));

        //var directSearch = trie.search(staff);

        //System.out.println("this works " + directSearch);

        var indirectSearch = trie.search(staff, yellow);

        System.out.println("this doesnt work " + indirectSearch);

    }

}
