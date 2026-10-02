package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Optional;
import java.util.function.Function;

public record WidgetModifier(int xOffset, int yOffset,
                             int width,
                             Optional<Integer> xFromCenter,
                             Optional<Boolean> visible,
                             Optional<String> message,
                             Optional<IntRange> targetX, Optional<IntRange> targetY,
                             Optional<IntRange> targetW, Optional<IntRange> targetH,
                             Optional<String> targetMessage,
                             Optional<String> targetMessageKey,
                             Optional<String> targetClass) {

    public static final Codec<WidgetModifier> CODEC = RecordCodecBuilder.<WidgetModifier>create(i -> i.group(
            Codec.INT.optionalFieldOf("x_offset", 0).forGetter(WidgetModifier::xOffset),
            Codec.INT.optionalFieldOf("y_offset", 0).forGetter(WidgetModifier::yOffset),
            Codec.INT.optionalFieldOf("width_increment", 0).forGetter(WidgetModifier::width),
            // absolute x from the screen center. unaffected by other mods moving the row around
            Codec.INT.optionalFieldOf("x_from_center").forGetter(WidgetModifier::xFromCenter),
            Codec.BOOL.optionalFieldOf("visible").forGetter(WidgetModifier::visible),
            Codec.STRING.optionalFieldOf("message").forGetter(WidgetModifier::message),
            IntRange.CODEC.optionalFieldOf("target_x").forGetter(WidgetModifier::targetX),
            IntRange.CODEC.optionalFieldOf("target_y").forGetter(WidgetModifier::targetY),
            IntRange.CODEC.optionalFieldOf("target_width").forGetter(WidgetModifier::targetW),
            IntRange.CODEC.optionalFieldOf("target_height").forGetter(WidgetModifier::targetH),
            Codec.STRING.optionalFieldOf("target_message").forGetter(WidgetModifier::targetMessage),
            // translation key. works in every language
            Codec.STRING.optionalFieldOf("target_message_key").forGetter(WidgetModifier::targetMessageKey),
            Codec.STRING.xmap(PlatStuff::maybeRemapName, PlatStuff::maybeRemapName).optionalFieldOf("target_class_name").forGetter(WidgetModifier::targetClass)
    ).apply(i, WidgetModifier::new)).comapFlatMap(o -> {
        if (o.targetW.isEmpty() && o.targetH.isEmpty() && o.targetX.isEmpty()
                && o.targetClass.isEmpty()
                && o.targetY.isEmpty() && o.targetMessage.isEmpty() && o.targetMessageKey.isEmpty()) {
            return DataResult.error(() -> "Widget modifier must have at least one target");
        }
        if (o.xFromCenter.isPresent() && o.xOffset != 0) {
            return DataResult.error(() -> "Widget modifier can't have both x_offset and x_from_center");
        }
        return DataResult.success(o);
    }, Function.identity());

    // True when this modifier's colormapToFill filters all match the widget (no mutation). Used by the editor overlay.
    public boolean matches(AbstractWidget widget) {
        if (targetX.isPresent() && !targetX.get().has(widget.getX())) return false;
        if (targetY.isPresent() && !targetY.get().has(widget.getY())) return false;
        if (targetH.isPresent() && !targetH.get().has(widget.getHeight())) return false;
        if (targetW.isPresent() && !targetW.get().has(widget.getWidth())) return false;
        if (targetMessage.isPresent() && !widget.getMessage().getString().equals(targetMessage.get())) return false;
        if (targetMessageKey.isPresent() && !(widget.getMessage().getContents() instanceof TranslatableContents tc
                && tc.getKey().equals(targetMessageKey.get()))) return false;
        if (targetClass.isPresent()) {
            String name = targetClass.get();
            if (!widget.getClass().getSimpleName().equals(name) &&
                    !widget.getClass().getName().equals(name)) return false;
        }
        return true;
    }

    public boolean maybeModify(AbstractWidget widget, int screenWidth) {
        if (!matches(widget)) return false;
        if (xFromCenter.isPresent()) widget.setX(screenWidth / 2 + xFromCenter.get());
        else widget.setX(widget.getX() + this.xOffset);
        widget.setY(widget.getY() + this.yOffset);
        widget.setWidth(widget.getWidth() + this.width);

        message.ifPresent(s -> widget.setMessage(Component.translatable(s)));
        visible.ifPresent(v -> widget.visible = v);
        return true;
    }
}
