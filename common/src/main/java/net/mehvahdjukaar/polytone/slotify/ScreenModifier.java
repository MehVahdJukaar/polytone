package net.mehvahdjukaar.polytone.slotify;

import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.utils.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ScreenModifier(int titleX, int titleY, int labelX, int labelY,
                             @Nullable Integer titleColor, @Nullable Integer labelColor,
                             List<SimpleSprite> extraRederables,
                             List<WidgetModifier> widgetModifiers,
                             Map<String, SpecialOffset> specialOffsets) {

    public ScreenModifier(GuiModifier original) {
        this(original.titleX(), original.titleY(), original.labelX(), original.labelY(),
                original.titleColor(), original.labelColor(),
                new ArrayList<>(original.sprites()),
                new ArrayList<>(original.widgetModifiers()),
                Map.copyOf(original.specialOffsets()));
    }

    public ScreenModifier merge(ScreenModifier newMod) {
        return new ScreenModifier(
                newMod.titleX != 0 ? newMod.titleX : this.titleX,
                newMod.titleY != 0 ? newMod.titleY : this.titleY,
                newMod.labelX != 0 ? newMod.labelX : this.labelX,
                newMod.labelY != 0 ? newMod.labelY : this.labelY,
                newMod.titleColor != null ? newMod.titleColor : this.titleColor,
                newMod.labelColor != null ? newMod.labelColor : this.labelColor,
                Utils.mergeList(newMod.extraRederables, this.extraRederables),
                Utils.mergeList(newMod.widgetModifiers, this.widgetModifiers),
                Utils.mergedMap(newMod.specialOffsets, this.specialOffsets)
        );
    }

    @Nullable
    public SpecialOffset getSpecial(String key) {
        return this.specialOffsets.get(key);
    }

    public void modifyWidgets(AbstractWidget button) {
        for (var m : this.widgetModifiers) {
            m.maybeModify(button);
        }
    }

    public void renderSprites(GuiGraphics poseStack) {
        RenderSystem.enableDepthTest();
        this.extraRederables.forEach(r -> r.render(poseStack));
    }
}
