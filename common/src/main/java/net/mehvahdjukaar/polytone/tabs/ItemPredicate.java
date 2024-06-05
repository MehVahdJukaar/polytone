package net.mehvahdjukaar.polytone.tabs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public interface ItemPredicate extends Predicate<ItemStack> {

    MapRegistry.CodecMap<ItemPredicate> TYPES = MapRegistry.ofCodec("Polytone Item Predicates");

    Codec<ItemPredicate> CODEC = TYPES.dispatch("type", ItemPredicate::getCodec, c -> c);


    MapCodec<? extends ItemPredicate> getCodec();


    True TRUE_PRED = new True();
    MapCodec<True> TRUE = TYPES.register("true", MapCodec.unit(TRUE_PRED));

    class True implements ItemPredicate {

        @Override
        public boolean test(ItemStack stack) {
            return true;
        }

        @Override
        public MapCodec<True> getCodec() {
            return TRUE;
        }
    }

    MapCodec<And> AND = TYPES.register("and",
            ItemPredicate.CODEC.listOf().fieldOf("predicates")
                    .xmap(And::new, And::predicates));

    record And(List<ItemPredicate> predicates) implements ItemPredicate {

        @Override
        public boolean test(ItemStack stack) {
            return predicates.stream().allMatch(p -> p.test(stack));
        }

        @Override
        public MapCodec<? extends ItemPredicate> getCodec() {
            return AND;
        }
    }

    MapCodec<Or> OR = TYPES.register("or",
            ItemPredicate.CODEC.listOf().fieldOf("predicates")
                    .xmap(Or::new, Or::predicates));

    record Or(List<ItemPredicate> predicates) implements ItemPredicate {

        @Override
        public boolean test(ItemStack stack) {
            return predicates.stream().anyMatch(p -> p.test(stack));
        }

        @Override
        public MapCodec<Or> getCodec() {
            return OR;
        }

    }

    MapCodec<Not> NOT = TYPES.register("not",
            ItemPredicate.CODEC.fieldOf("predicate")
                    .xmap(Not::new, Not::predicate));

    record Not(ItemPredicate predicate) implements ItemPredicate {

        @Override
        public boolean test(ItemStack stack) {
            return !predicate.test(stack);
        }

        @Override
        public MapCodec<Not> getCodec() {
            return NOT;
        }
    }


    MapCodec<TagMatch> TAG_MATCH = TYPES.register("tag_match",
            TagKey.codec(Registries.ITEM).fieldOf("tag")
                    .xmap(TagMatch::new, TagMatch::tag));

    record TagMatch(TagKey<Item> tag) implements ItemPredicate {

        @Override
        public boolean test(ItemStack stack) {
            return stack.is(tag);
        }

        @Override
        public MapCodec<TagMatch> getCodec() {
            return TAG_MATCH;
        }
    }

    MapCodec<ItemMatch> ITEM_MATCH = TYPES.register("items_match",
            BuiltInRegistries.ITEM.byNameCodec().listOf().fieldOf("items")
                    .xmap(ItemMatch::new, ItemMatch::items));

    record ItemMatch(List<Item> items) implements ItemPredicate {

        @Override
        public boolean test(ItemStack stack) {
            return items.contains(stack.getItem());
        }

        @Override
        public MapCodec<ItemMatch> getCodec() {
            return ITEM_MATCH;
        }
    }

    MapCodec<ItemStackMatch> ITEMSTACK_MATCH = TYPES.register("itemstack_match",
            ItemStack.SINGLE_ITEM_CODEC.fieldOf("itemstack")
                    .xmap(ItemStackMatch::new, ItemStackMatch::items));

    record ItemStackMatch(ItemStack items) implements ItemPredicate {

        @Override
        public boolean test(ItemStack stack) {
            return ItemStack.matches(items, stack);
        }

        @Override
        public MapCodec<ItemStackMatch> getCodec() {
            return ITEMSTACK_MATCH;
        }
    }


    Pattern TRUE_PATTERN = Pattern.compile(".*");
    MapCodec<IDMatch> ID_MATCH = TYPES.register("id_match",
            RecordCodecBuilder.mapCodec(i -> i.group(
                    ExtraCodecs.PATTERN.optionalFieldOf("namespace", TRUE_PATTERN).forGetter(IDMatch::namespace),
                    ExtraCodecs.PATTERN.optionalFieldOf("path", TRUE_PATTERN).forGetter(IDMatch::path)
            ).apply(i, IDMatch::new)));

    record IDMatch(Pattern namespace, Pattern path) implements ItemPredicate {

        @Override
        public boolean test(ItemStack stack) {
            Item item = stack.getItem();
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            return namespace.matcher(id.getNamespace()).matches() && path.matcher(id.getPath()).matches();
        }

        @Override
        public MapCodec<IDMatch> getCodec() {
            return ID_MATCH;
        }
    }

}

