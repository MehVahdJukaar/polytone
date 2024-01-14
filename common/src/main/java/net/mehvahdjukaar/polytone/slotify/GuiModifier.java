package net.mehvahdjukaar.polytone.slotify;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

//instance persists just during deserialization. we could have used decoder only
public record GuiModifier(Type type, String target,
                          List<SlotModifier> slotModifiers,
                          int titleX, int titleY, int labelX, int labelY,
                          List<SimpleSprite> sprites,
                          List<WidgetModifier> widgetModifiers,
                          Map<String, SpecialOffset> specialOffsets) {

    public static final Codec<GuiModifier> CODEC =
            RecordCodecBuilder.<GuiModifier>create(i -> i.group(
                    StringRepresentable.fromEnum(Type::values).fieldOf("target_type").forGetter(GuiModifier::type),
                    Codec.STRING.fieldOf("target").forGetter(GuiModifier::target),
                    SlotModifier.CODEC.listOf().optionalFieldOf("slot_modifiers", List.of()).forGetter(GuiModifier::slotModifiers),
                    Codec.INT.optionalFieldOf("title_x_offset", 0).forGetter(GuiModifier::titleX),
                    Codec.INT.optionalFieldOf("title_y_offset", 0).forGetter(GuiModifier::titleY),
                    Codec.INT.optionalFieldOf("label_x_offset", 0).forGetter(GuiModifier::labelX),
                    Codec.INT.optionalFieldOf("label_y_offset", 0).forGetter(GuiModifier::labelY),
                    SimpleSprite.CODEC.listOf().optionalFieldOf("sprites", List.of()).forGetter(GuiModifier::sprites),
                    WidgetModifier.CODEC.listOf().optionalFieldOf("widget_modifiers", List.of()).forGetter(GuiModifier::widgetModifiers),
                    Codec.unboundedMap(Codec.STRING, SpecialOffset.CODEC).optionalFieldOf("special_offsets", Map.of()).forGetter(GuiModifier::specialOffsets)

            ).apply(i, GuiModifier::new)).comapFlatMap((instance) -> {
                if (instance.type == Type.MENU_ID) {
                    var error = ResourceLocation.read(instance.target).error();
                    if (error.isPresent()) return DataResult.error(() -> error.get().message());
                }
                if ((instance.type == Type.SCREEN_CLASS || instance.type == Type.SCREEN_TITLE) &&
                        instance.slotModifiers.stream().anyMatch(SlotModifier::hasOffset)) {
                    return DataResult.error(() -> "Slot modifiers cannot alter position when using a screen_class or screen_title target_type. Use menu_id or menu_class instead");
                }
                return DataResult.success(instance);
            }, Function.identity());



    public enum Type implements StringRepresentable {
        MENU_ID,
        MENU_CLASS,
        SCREEN_CLASS,
        SCREEN_TITLE;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public boolean targetsClass() {
        return type != Type.MENU_ID && type != Type.SCREEN_TITLE;
    }

    public boolean targetsMenuId() {
        return type == Type.MENU_ID;
    }



}

