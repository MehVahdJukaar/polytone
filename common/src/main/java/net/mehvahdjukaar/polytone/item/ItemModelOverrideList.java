package net.mehvahdjukaar.polytone.item;

import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.utils.DepthSearchTrie;
import net.mehvahdjukaar.polytone.utils.FrequencyOrderedCollection;
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
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        this.overrides.acceptEntries(this.entries);
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
        return this.overrides.search(stack);
    }

    public int size() {
        return entries.size();
    }


    public static class PropertiesSearchTrie extends DepthSearchTrie<Object, Object, BakedModel, ItemStack> {

        private final List<DataComponentType<?>> orderedKeys = new ArrayList<>();

        @Override
        public void clear() {
            super.clear();
            this.orderedKeys.clear();
        }


        @Override
        protected Object getKeyOfType(Object folder) {
            if (folder instanceof TypedDataComponent<?> t) {
                return t.type();
            }
            if (folder instanceof Integer) {
                return Integer.class;
            }
            return folder;
        }

        @Override
        protected Object getKeyFromType(Object type, ItemStack stack) {
            if (type instanceof DataComponentType<?> t) {
                return stack.getComponents().getTyped(t);
            } else if (type == Integer.class) {
                return stack.getCount();
            }
            return null;
        }

        public void acceptEntries(List<ItemModelOverride> entries) {
            boolean hasCount = false;
            FrequencyOrderedCollection<DataComponentType<?>> keyFrequencies = new FrequencyOrderedCollection<>();
            for (ItemModelOverride entry : entries) {
                if (entry.hasStackCount()) hasCount = true;
                for (var component : entry.components()) {
                    keyFrequencies.add(component.type());
                }
            }
            // assumes keys with more values will be more common. actually inverse to minimize space
            this.orderedKeys.addAll(keyFrequencies.stream().toList());

            for (ItemModelOverride entry : entries) {
                List<Object> key = new ArrayList<>();
                if (hasCount) key.add(entry.stackCount());
                for (DataComponentType<?> type : this.orderedKeys) {
                    key.add(entry.components().getTyped(type));
                }
                this.insert(key, PlatStuff.getBakedModel(entry.model()));
            }
        }

    }

    public static void testTrie() {
        if (true) return;
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
                        .set(DataComponents.CUSTOM_NAME, Component.literal("sword"))
                        .build(),
                ResourceLocation.tryParse("blue_sword_test")
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
        trie.acceptEntries(list);


        // Print the trie structure
        System.out.println("Trie structure after insertions:");
        trie.printTrie();

        trie.optimizeTree();

        trie.printTrie();

        TypedDataComponent<DyeColor> red = new TypedDataComponent<>(DataComponents.BASE_COLOR, DyeColor.RED);
        //blue
        TypedDataComponent<DyeColor> blue = new TypedDataComponent<>(DataComponents.BASE_COLOR, DyeColor.BLUE);
        TypedDataComponent<DyeColor> yellow = new TypedDataComponent<>(DataComponents.BASE_COLOR, DyeColor.YELLOW);
        TypedDataComponent<Integer> one = new TypedDataComponent<>(DataComponents.MAX_STACK_SIZE, 1);
        TypedDataComponent<Component> staff = new TypedDataComponent<>(DataComponents.CUSTOM_NAME, Component.literal("staff"));
        TypedDataComponent<Component> spear = new TypedDataComponent<>(DataComponents.CUSTOM_NAME, Component.literal("spear"));
        TypedDataComponent<Component> sword = new TypedDataComponent<>(DataComponents.CUSTOM_NAME, Component.literal("sword"));

        //var directSearch = trie.search(staff);

        //System.out.println("this works " + directSearch);

        ItemStack s = Items.DIAMOND.getDefaultInstance();
        s.set(staff.type(), staff.value());
        //var indirectSearch = trie.get(s);
        s.set(one.type(), one.value());

        var search2 = trie.search(s);

        s.set(spear.type(), spear.value());
        var search3 = trie.search(s);

        ItemStack ss = Items.EMERALD.getDefaultInstance();
        ss.set(sword.type(), sword.value());
        var normalSword = trie.search(ss);

        s.set(sword.type(), sword.value());
        s.set(one.type(), one.value());
        var otherSpear = trie.search(s);

        s.set(blue.type(), blue.value());
        var blueSpear = trie.search(s);

        System.out.println("this work " + search2);

    }

}
