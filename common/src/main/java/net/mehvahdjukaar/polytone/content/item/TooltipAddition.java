package net.mehvahdjukaar.polytone.content.item;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public record TooltipAddition(Component component, int position) {

    public static final Codec<TooltipAddition> CODEC = Codec.withAlternative(
            SchemaRecord.create(TooltipAddition.class, i -> i.group(
                    i.field("component", ComponentSerialization.CODEC, TooltipAddition::component),
                    i.field("position", Codec.INT, TooltipAddition::position)
            ).apply(i, TooltipAddition::new)),
            ComponentSerialization.CODEC.xmap(c -> new TooltipAddition(c, Integer.MAX_VALUE), TooltipAddition::component));
}
