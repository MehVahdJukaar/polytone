package net.mehvahdjukaar.polytone.tabs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public interface ItemPredicate extends Predicate<ItemStack> {

    MapRegistry.CodecMap<ItemPredicate> TYPES = MapRegistry.ofCodec("Polytone Item Predicates");

    Codec<ItemPredicate> CODEC = TYPES.dispatch("type", ItemPredicate::getCodec, Function.identity());


    Codec<? extends ItemPredicate> getCodec();


    True TRUE_PRED = new True();
    Codec<True> TRUE = TYPES.register("true", Codec.unit(TRUE_PRED));

    class True implements ItemPredicate {

        @Override
        public boolean test(ItemStack stack) {
            return true;
        }

        @Override
        public Codec<True> getCodec() {
            return TRUE;
        }
    }

    Codec<And> AND = TYPES.register("and",
            ItemPredicate.CODEC.listOf().fieldOf("predicates")
                    .xmap(And::new, And::predicates).codec());

    record And(List<ItemPredicate> predicates) implements ItemPredicate {

        @Override
        public boolean test(ItemStack stack) {
            return predicates.stream().allMatch(p -> p.test(stack));
        }

        @Override
        public Codec<? extends ItemPredicate> getCodec() {
            return AND;
        }
    }

    Codec<Or> OR = TYPES.register("or",
            ItemPredicate.CODEC.listOf().fieldOf("predicates")
                    .xmap(Or::new, Or::predicates).codec());

    record Or(List<ItemPredicate> predicates) implements ItemPredicate {

        @Override
        public boolean test(ItemStack stack) {
            return predicates.stream().anyMatch(p -> p.test(stack));
        }

        @Override
        public Codec<Or> getCodec() {
            return OR;
        }

    }

    Codec<Not> NOT = TYPES.register("not",
            ItemPredicate.CODEC.fieldOf("predicate")
                    .xmap(Not::new, Not::predicate).codec());

    record Not(ItemPredicate predicate) implements ItemPredicate {

        @Override
        public boolean test(ItemStack stack) {
            return !predicate.test(stack);
        }

        @Override
        public Codec<Not> getCodec() {
            return NOT;
        }
    }


    Codec<TagMatch> TAG_MATCH = TYPES.register("tag_match",
            TagKey.codec(Registries.ITEM).fieldOf("tag")
                    .xmap(TagMatch::new, TagMatch::tag).codec());

    record TagMatch(TagKey<Item> tag) implements ItemPredicate {

        @Override
        public boolean test(ItemStack stack) {
            return stack.is(tag);
        }

        @Override
        public Codec<TagMatch> getCodec() {
            return TAG_MATCH;
        }
    }

    Codec<ItemMatch> ITEM_MATCH = TYPES.register("items_match",
            BuiltInRegistries.ITEM.byNameCodec().listOf().fieldOf("items")
                    .xmap(ItemMatch::new, ItemMatch::items).codec());

    record ItemMatch(List<Item> items) implements ItemPredicate {

        @Override
        public boolean test(ItemStack stack) {
            return items.contains(stack.getItem());
        }

        @Override
        public Codec<ItemMatch> getCodec() {
            return ITEM_MATCH;
        }
    }

    Codec<ItemStackMatch> ITEMSTACK_MATCH = TYPES.register("itemstack_match",
            ExtraItemCodecs.ITEMSTACK.fieldOf("itemstack")
                    .xmap(ItemStackMatch::new, ItemStackMatch::items).codec());

    record ItemStackMatch(ItemStack items) implements ItemPredicate {

        @Override
        public boolean test(ItemStack stack) {
            return ItemStack.matches(items, stack);
        }

        @Override
        public Codec<ItemStackMatch> getCodec() {
            return ITEMSTACK_MATCH;
        }
    }


    Pattern TRUE_PATTERN = Pattern.compile(".*");
    Codec<IDMatch> ID_MATCH = TYPES.register("id_match",
            RecordCodecBuilder.create(i -> i.group(
                    StrOpt.of(ExtraCodecs.PATTERN, "namespace", TRUE_PATTERN).forGetter(IDMatch::namespace),
                    StrOpt.of(ExtraCodecs.PATTERN, "path", TRUE_PATTERN).forGetter(IDMatch::path)
            ).apply(i, IDMatch::new)));

    record IDMatch(Pattern namespace, Pattern path) implements ItemPredicate {

        @Override
        public boolean test(ItemStack stack) {
            Item item = stack.getItem();
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            return namespace.matcher(id.getNamespace()).matches() && path.matcher(id.getPath()).matches();
        }

        @Override
        public Codec<IDMatch> getCodec() {
            return ID_MATCH;
        }
    }

}

