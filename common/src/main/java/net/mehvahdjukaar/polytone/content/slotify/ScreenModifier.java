package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.utils.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ScreenModifier(int titleX, int titleY, int labelX, int labelY,
                             int xOff, int yOff, int wOff, int hOff,
                             @Nullable Integer titleColor, @Nullable Integer labelColor,
                             List<Renderable> extraRenderables,
                             List<WidgetModifier> widgetModifiers,
                             Map<String, SpecialOffset> specialOffsets) {

    public static ScreenModifier fromGuiMod(GuiModifier original) {
        List<Renderable> lis = new ArrayList<>(original.sprites());
        lis.addAll(original.textList());
        return new ScreenModifier(original.titleX(), original.titleY(), original.labelX(), original.labelY(),
                original.xOff(), original.yOff(), original.wOff(), original.hOff(),
                original.titleColor(), original.labelColor(),
                lis,
                new ArrayList<>(original.widgetModifiers()),
                Map.copyOf(original.specialOffsets()));
    }

    public ScreenModifier merge(ScreenModifier newMod) {
        return new ScreenModifier(
                newMod.titleX != 0 ? newMod.titleX : this.titleX,
                newMod.titleY != 0 ? newMod.titleY : this.titleY,
                newMod.labelX != 0 ? newMod.labelX : this.labelX,
                newMod.labelY != 0 ? newMod.labelY : this.labelY,
                newMod.xOff != 0 ? newMod.xOff : this.xOff,
                newMod.yOff != 0 ? newMod.yOff : this.yOff,
                newMod.wOff != 0 ? newMod.wOff : this.wOff,
                newMod.hOff != 0 ? newMod.hOff : this.hOff,
                newMod.titleColor != null ? newMod.titleColor : this.titleColor,
                newMod.labelColor != null ? newMod.labelColor : this.labelColor,
                Utils.mergeList(newMod.extraRenderables, this.extraRenderables),
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

    public void renderExtrs(GuiGraphics poseStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.enableDepthTest();
        this.extraRenderables.forEach(r -> r.render(poseStack, mouseX, mouseY, partialTicks));
    }
}
