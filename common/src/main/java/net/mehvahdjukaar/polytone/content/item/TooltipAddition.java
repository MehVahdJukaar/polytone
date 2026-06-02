package net.mehvahdjukaar.polytone.content.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.content.tabs.CreativeTabModifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public record TooltipAddition(Component component, int position) {

    public static final Codec<TooltipAddition> CODEC = Codec.withAlternative(
            RecordCodecBuilder.create(instance -> instance.group(
                    CreativeTabModifier.COMPONENT_CODEC.fieldOf("component").forGetter(TooltipAddition::component),
                    Codec.INT.fieldOf("position").forGetter(TooltipAddition::position)
            ).apply(instance, TooltipAddition::new)),
            ComponentSerialization.CODEC.xmap(c -> new TooltipAddition(c, Integer.MAX_VALUE), TooltipAddition::component));
}
