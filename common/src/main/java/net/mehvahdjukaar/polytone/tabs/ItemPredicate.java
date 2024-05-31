package net.mehvahdjukaar.polytone.tabs;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;
import java.util.function.Predicate;

public interface ItemPredicate extends Predicate<ItemStack> {

    BiMap<String, Codec<? extends ItemPredicate>> predicates = HashBiMap.create();

    Codec<Codec<? extends ItemPredicate>> TYPE_CODEC = Codec.STRING.xmap(predicates::get, s -> predicates.inverse().get(s));

    Codec<ItemPredicate> CODEC = TYPE_CODEC.dispatch("type", ItemPredicate::getCodec, Function.identity());
    ItemPredicate TRUE = new ItemPredicate() {
        @Override
        public Codec<? extends ItemPredicate> getCodec() {
            return null;
        }

        @Override
        public boolean test(ItemStack stack) {
            return true;
        }
    };

    Codec<? extends ItemPredicate> getCodec();


    static <T extends ItemPredicate> Codec<T> register(String name, Codec<T> predicate) {
        predicates.put(name, predicate);
        return predicate;
    }


    Codec<ItemPredicate> OR = register("or", Codec.unit(new ItemPredicate() {
        @Override
        public boolean test(ItemStack stack) {
            return false;
        }

        @Override
        public Codec<ItemPredicate> getCodec() {
            return OR;
        }
    }));

    Codec<ItemPredicate> AND = register("and", Codec.unit(new ItemPredicate() {
        @Override
        public boolean test(ItemStack stack) {
            return false;
        }

        @Override
        public Codec<ItemPredicate> getCodec() {
            return AND;
        }
    }));

    Codec<ItemPredicate> NOT = register("not", Codec.unit(new ItemPredicate() {
        @Override
        public boolean test(ItemStack stack) {
            return false;
        }

        @Override
        public Codec<ItemPredicate> getCodec() {
            return NOT;
        }
    }));


    Codec<TagMatch> TAG_MATCH = ItemPredicate.register("tag_match",
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

}
