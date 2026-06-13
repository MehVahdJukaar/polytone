package net.mehvahdjukaar.polytone.common.codec_ui.example;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaRecord;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * MC game-type editor: edit a (simplified) {@link ItemStack}.
 *
 * <p>Pattern: <b>wrapper</b>. Vanilla {@code ItemStack.CODEC} is a dispatch over
 * {@code DataComponents} — a deeply-nested, type-safe map keyed by {@link
 * net.minecraft.core.component.DataComponentType} entries. Modelling that in a static schema
 * is out of scope for a demo, so we expose only the two fields most users care about: the
 * item id (a registry pick from {@link Registries#ITEM}) and a stack count.
 *
 * <p>{@code ItemStack.SIMPLE_ITEM_CODEC} would have let us go companion-style, but it encodes
 * as just a bare string with no count, which is too minimal to demonstrate the editor. The
 * wrapper holds parsed values and round-trips through its own codec — JSON is
 * {@code {"id": "minecraft:diamond", "count": 1}} which does NOT match any vanilla codec
 * verbatim. Convert to a real {@link ItemStack} via {@link #toItemStack()}.
 *
 * <p>Skipped: all {@code DataComponents} (custom name, enchantments, lore, NBT, etc.).
 */
public record ItemStackExample(Item id, int count) {

    /** Convenience: build a real {@link ItemStack} from this wrapper. */
    public ItemStack toItemStack() {
        return new ItemStack(id, count);
    }

    /** Convenience: read an {@link ItemStack}'s item + count into a wrapper instance. */
    public static ItemStackExample of(ItemStack stack) {
        return new ItemStackExample(stack.getItem(), stack.getCount());
    }

    public static final SchemaCodec<ItemStackExample> SCHEMA_CODEC = SchemaRecord.create(
            ItemStackExample.class, i -> i.group(
                    i.field("id",
                            SchemaCodecs.registryEntry(Registries.ITEM, BuiltInRegistries.ITEM.byNameCodec()),
                            ItemStackExample::id),
                    i.optional("count", Codec.intRange(1, 99), 1, ItemStackExample::count)
            ).apply(i, ItemStackExample::new));
}
