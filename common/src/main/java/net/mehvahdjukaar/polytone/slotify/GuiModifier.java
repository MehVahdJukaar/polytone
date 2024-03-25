package net.mehvahdjukaar.polytone.slotify;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

//instance persists just during deserialization. we could have used decoder only
public record GuiModifier(Type type, String target,
                          List<SlotModifier> slotModifiers,
                          int titleX, int titleY, int labelX, int labelY,
                          @Nullable Integer titleColor, @Nullable Integer labelColor,
                          List<SimpleSprite> sprites,
                          List<WidgetModifier> widgetModifiers,
                          Map<String, SpecialOffset> specialOffsets) {

    public static final Codec<GuiModifier> CODEC =
            RecordCodecBuilder.<GuiModifier>create(i -> i.group(
                    StringRepresentable.fromEnum(Type::values).fieldOf("target_type").forGetter(GuiModifier::type),
                    Codec.STRING.xmap(PlatStuff::maybeRemapName, PlatStuff::maybeRemapName).fieldOf("target").forGetter(GuiModifier::target),
                    StrOpt.of(SlotModifier.CODEC.listOf(), "slot_modifiers", List.of()).forGetter(GuiModifier::slotModifiers),
                    StrOpt.of(Codec.INT, "title_x_offset", 0).forGetter(GuiModifier::titleX),
                    StrOpt.of(Codec.INT, "title_y_offset", 0).forGetter(GuiModifier::titleY),
                    StrOpt.of(Codec.INT, "label_x_offset", 0).forGetter(GuiModifier::labelX),
                    StrOpt.of(Codec.INT, "label_y_offset", 0).forGetter(GuiModifier::labelY),
                    StrOpt.of(ColorUtils.CODEC, "title_color", null).forGetter(GuiModifier::titleColor),
                    StrOpt.of(ColorUtils.CODEC, "label_color", null).forGetter(GuiModifier::labelColor),
                    StrOpt.of(SimpleSprite.CODEC.listOf(), "sprites", List.of()).forGetter(GuiModifier::sprites),
                    StrOpt.of(WidgetModifier.CODEC.listOf(), "widget_modifiers", List.of()).forGetter(GuiModifier::widgetModifiers),
                    StrOpt.of(Codec.unboundedMap(Codec.STRING, SpecialOffset.CODEC), "special_offsets", Map.of()).forGetter(GuiModifier::specialOffsets)

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

