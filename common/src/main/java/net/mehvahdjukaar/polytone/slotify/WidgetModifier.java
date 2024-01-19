package net.mehvahdjukaar.polytone.slotify;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.function.Function;

public record WidgetModifier(int xOffset, int yOffset,
                             int width,
                             Optional<String> message,
                             Optional<Integer> targetX, Optional<Integer> targetY,
                             Optional<Integer> targetW, Optional<Integer> targetH,
                             Optional<String> targetMessage,
                             Optional<String> targetClass) {

    public static final Codec<WidgetModifier> CODEC = RecordCodecBuilder.<WidgetModifier>create(i -> i.group(
            StrOpt.of(Codec.INT,"x_offset", 0).forGetter(WidgetModifier::xOffset),
            StrOpt.of(Codec.INT,"y_offset", 0).forGetter(WidgetModifier::yOffset),
            StrOpt.of(Codec.INT,"width_increment", 0).forGetter(WidgetModifier::width),
            StrOpt.of(Codec.STRING,"message").forGetter(WidgetModifier::message),
            StrOpt.of(Codec.INT,"target_x").forGetter(WidgetModifier::targetX),
            StrOpt.of(Codec.INT,"target_y").forGetter(WidgetModifier::targetY),
            StrOpt.of(Codec.INT,"target_width").forGetter(WidgetModifier::targetY),
            StrOpt.of(Codec.INT,"target_height").forGetter(WidgetModifier::targetY),
            StrOpt.of(Codec.STRING,"target_message").forGetter(WidgetModifier::targetMessage),
            StrOpt.of(Codec.STRING.xmap(PlatStuff::maybeRemapName, PlatStuff::maybeRemapName),"target_class_name").forGetter(WidgetModifier::targetClass)
    ).apply(i, WidgetModifier::new)).comapFlatMap(o -> {
        if (o.targetW.isEmpty() && o.targetH.isEmpty() && o.targetX.isEmpty() && o.targetY.isEmpty() && o.targetMessage.isEmpty()) {
            return DataResult.error(() -> "Widget modifier must have at least one target");
        }
        return DataResult.success(o);
    }, Function.identity());

    public void maybeModify(AbstractWidget widget) {
        if (targetX.isPresent() && widget.getX() != targetX.get()) return;
        if (targetY.isPresent() && widget.getY() != targetY.get()) return;
        if (targetH.isPresent() && widget.getHeight() != targetH.get()) return;
        if (targetW.isPresent() && widget.getWidth() != targetW.get()) return;
        if (targetMessage.isPresent() && !widget.getMessage().getString().equals(targetMessage.get())) return;
        if (targetClass.isPresent()) {
            String name = targetClass.get();
            if (!widget.getClass().getSimpleName().equals(name) &&
                    !widget.getClass().getName().equals(name)) return;
        }
        widget.setX(widget.getX() + this.xOffset);
        widget.setY(widget.getY() + this.yOffset);
        widget.setWidth(widget.getWidth() + this.width);

        message.ifPresent(s -> widget.setMessage(Component.translatable(s)));
    }
}
