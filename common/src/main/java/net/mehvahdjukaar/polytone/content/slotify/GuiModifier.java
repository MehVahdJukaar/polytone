package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

//instance persists just during deserialization. we could have used decoder only
public record GuiModifier(Type type, String target,
                          List<SlotModifier> slotModifiers,
                          int titleX, int titleY, int labelX, int labelY,
                          int xOff, int yOff, int wOff, int hOff,
                          @Nullable Integer titleColor, @Nullable Integer labelColor,
                          List<SimpleSprite> sprites,
                          List<SimpleText> textList,
                          List<WidgetModifier> widgetModifiers,
                          Map<String, SpecialOffset> specialOffsets) {

    public GuiModifier(Type type, String target, List<SlotModifier> slotModifiers,
                       int titleX, int titleY, int labelX, int labelY,
                       int xOff, int yOff, int wOff, int hOff,
                       Optional<Integer> titleColor, Optional<Integer> labelColor,
                       List<SimpleSprite> sprites, List<SimpleText> textList,
                       List<WidgetModifier> widgetModifiers,
                       Map<String, SpecialOffset> specialOffsets) {
        this(type, target, slotModifiers, titleX, titleY, labelX, labelY,
                xOff, yOff, wOff, hOff,
                titleColor.orElse(null), labelColor.orElse(null), sprites, textList, widgetModifiers, specialOffsets);
    }


    public enum Type implements StringRepresentable {
        MENU_ID,
        MENU_CLASS,
        SCREEN_CLASS,
        SCREEN_TITLE;

        @Override
        public @NonNull String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    private static final SchemaCodec<GuiModifier> RECORD_CODEC =
            SchemaRecord.create(GuiModifier.class, i -> i.group(
                    i.field("target_type", StringRepresentable.fromEnum(Type::values), GuiModifier::type),
                    i.field("target", Codec.STRING.xmap(PlatStuff::maybeRemapName, PlatStuff::maybeRemapName), GuiModifier::target),
                    i.optional("slot_modifiers", SlotModifier.CODEC.listOf(), List.of(), GuiModifier::slotModifiers),
                    i.optional("title_x_offset", Codec.INT, 0, GuiModifier::titleX),
                    i.optional("title_y_offset", Codec.INT, 0, GuiModifier::titleY),
                    i.optional("label_x_offset", Codec.INT, 0, GuiModifier::labelX),
                    i.optional("label_y_offset", Codec.INT, 0, GuiModifier::labelY),
                    i.optional("x_offset", Codec.INT, 0, GuiModifier::xOff),
                    i.optional("y_offset", Codec.INT, 0, GuiModifier::yOff),
                    i.optional("width_offset", Codec.INT, 0, GuiModifier::wOff),
                    i.optional("height_offset", Codec.INT, 0, GuiModifier::hOff),
                    i.optional("title_color", ColorUtils.COLOR, g -> Optional.ofNullable(g.titleColor)),
                    i.optional("label_color", ColorUtils.COLOR, g -> Optional.ofNullable(g.labelColor)),
                    i.optional("sprites", SimpleSprite.CODEC.listOf(), List.of(), GuiModifier::sprites),
                    i.optional("texts", SimpleText.CODEC.listOf(), List.of(), GuiModifier::textList),
                    i.optional("widget_modifiers", WidgetModifier.CODEC.listOf(), List.of(), GuiModifier::widgetModifiers),
                    i.optional("special_offsets", Codec.unboundedMap(Codec.STRING, SpecialOffset.CODEC), Map.of(), GuiModifier::specialOffsets)
            ).apply(i, GuiModifier::new));

    // decode-side validation on top of the record codec; SchemaCodec.lazy keeps the schema view
    public static final SchemaCodec<GuiModifier> CODEC = SchemaCodec.lazy(
            RECORD_CODEC.comapFlatMap((instance) -> {
                if (instance.type == Type.MENU_ID) {
                    var error = Identifier.read(instance.target).error();
                    if (error.isPresent()) return DataResult.error(() -> error.get().message());
                }
                if ((instance.type == Type.SCREEN_CLASS || instance.type == Type.SCREEN_TITLE) &&
                        instance.slotModifiers.stream().anyMatch(SlotModifier::hasOffset)) {
                    return DataResult.error(() -> "Slot modifiers cannot alter position when using a screen_class or screen_title target_type. Use menu_id or menu_class instead");
                }
                return DataResult.success(instance);
            }, Function.identity()),
            RECORD_CODEC::schema);


    public boolean targetsClass() {
        return type != Type.MENU_ID && type != Type.SCREEN_TITLE;
    }

    public boolean targetsMenuId() {
        return type == Type.MENU_ID;
    }


}

