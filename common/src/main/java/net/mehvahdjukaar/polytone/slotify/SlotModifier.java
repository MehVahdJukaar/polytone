package net.mehvahdjukaar.polytone.slotify;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public record SlotModifier(Optional<IntRange> targets, int color, int color2, int xOffset, int yOffset, int zOffset,
                           Optional<IntRange> targetX, Optional<IntRange> targetY, Optional<String> targetClass) {

    public static final Codec<SlotModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            StrOpt.of(TargetSlots.CODEC, "slots").forGetter(SlotModifier::targets),
            StrOpt.of(ColorUtils.CODEC, "color", -1).forGetter(SlotModifier::color),
            StrOpt.of(ColorUtils.CODEC, "color_2", -1).forGetter(SlotModifier::color2),
            StrOpt.of(Codec.INT, "x_offset", 0).forGetter(SlotModifier::xOffset),
            StrOpt.of(Codec.INT, "y_offset", 0).forGetter(SlotModifier::yOffset),
            StrOpt.of(Codec.INT, "z_offset", 0).forGetter(SlotModifier::zOffset),
            StrOpt.of(Codec.INT, "target_x").forGetter(SlotModifier::targetX),
            StrOpt.of(Codec.INT, "target_y").forGetter(SlotModifier::targetY),
            StrOpt.of(Codec.STRING.xmap(PlatStuff::maybeRemapName, PlatStuff::maybeRemapName), "target_class_name").forGetter(SlotModifier::targetClass)
    ).apply(i, SlotModifier::new));

    public void modify(Slot slot) {

        slot.x += this.xOffset;
        slot.y += this.yOffset;
    }


    public boolean matches(Slot slot) {
        if (targets.isPresent() && !targets.get().has(slot.index)) return false;
        if (targetX.isPresent() && targetX.get().has(slot.x)) return false;
        if (targetY.isPresent() && targetY.get().has(slot.y)) return false;
        if (targetClass.isPresent()) {
            String name = targetClass.get();
            if (!slot.getClass().getSimpleName().equals(name) &&
                    !slot.getClass().getName().equals(name)) return false;
        }
        return true;
    }

    public boolean hasCustomColor() {
        return color != -1 || color2 != -1 || zOffset != 0;
    }

    public void renderCustomHighlight(GuiGraphics graphics, int x, int y, int offset) {
        int c1 = color;
        int c2 = color2 == -1 ? color : color2;
        renderSlotHighlight(graphics, x, y, c1, c2, offset + zOffset);
    }

    public static void renderSlotHighlight(GuiGraphics graphics, int x, int y,
                                           int slotColor, int slotColor2, int offset) {
        RenderSystem.disableDepthTest();
        RenderSystem.colorMask(true, true, true, false);
        graphics.fillGradient(x, y, x + 16, y + 16, slotColor, slotColor2, offset);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
    }

    public boolean hasOffset() {
        return xOffset != 0 || yOffset != 0;
    }

}
