package net.mehvahdjukaar.polytone.content.slotify;

import net.mehvahdjukaar.polytone.common.expressions.impl.SimpleExp;
import net.mehvahdjukaar.polytone.common.struc.ListUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public record ScreenModifier(int titleX, int titleY, int labelX, int labelY,
                             int xOff, int yOff, int wOff, int hOff,
                             @Nullable Integer titleColor, @Nullable Integer labelColor,
                             List<Renderable> extraRenderables,
                             List<WidgetModifier> widgetModifiers,
                             Map<String, SpecialOffset> specialOffsets,
                             @Nullable SimpleExp condition) {

    public static ScreenModifier fromGuiMod(GuiModifier original) {
        List<Renderable> lis = new ArrayList<>(original.sprites());
        lis.addAll(original.textList());
        return new ScreenModifier(original.titleX(), original.titleY(), original.labelX(), original.labelY(),
                original.xOff(), original.yOff(), original.wOff(), original.hOff(),
                original.titleColor(), original.labelColor(),
                lis,
                new ArrayList<>(original.widgetModifiers()),
                Map.copyOf(original.specialOffsets()),
                original.condition());
    }

    public boolean passesCondition() {
        if (condition == null) return true;
        try {
            return condition.evaluate() != 0;
        } catch (Exception e) {
            // no player or level outside a world
            return false;
        }
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
                ListUtils.mergeList(newMod.extraRenderables, this.extraRenderables),
                ListUtils.mergeList(newMod.widgetModifiers, this.widgetModifiers),
                ListUtils.mergedMap(newMod.specialOffsets, this.specialOffsets),
                // conditions are already evaluated before merging, so the merged result carries none
                null
        );
    }

    @Nullable
    public SpecialOffset getSpecial(String key) {
        return this.specialOffsets.get(key);
    }

    // what our modifiers did to a widget and where that left it
    private static final class Applied {
        int dx, dy, dw;
        int x, y, w;
        @Nullable
        Integer fromCenter;
        @Nullable
        Boolean visible;
    }

    private static final Map<AbstractWidget, Applied> MODIFIED = new WeakHashMap<>();

    // can be called more than once. layout screens reset widgets on resize so we re-add only what they undid
    public void modifyWidgets(AbstractWidget button, int screenWidth) {
        Applied a = MODIFIED.get(button);
        if (a == null) {
            int x = button.getX(), y = button.getY(), w = button.getWidth();
            boolean matched = false;
            boolean fromCenter = false;
            Boolean visible = null;
            for (var mod : this.widgetModifiers) {
                if (!mod.maybeModify(button, screenWidth)) continue;
                matched = true;
                if (mod.xFromCenter().isPresent()) fromCenter = true;
                if (mod.visible().isPresent()) visible = mod.visible().get();
            }
            if (!matched) return;
            a = new Applied();
            a.dx = button.getX() - x;
            a.dy = button.getY() - y;
            a.dw = button.getWidth() - w;
            a.fromCenter = fromCenter ? button.getX() - screenWidth / 2 : null;
            a.visible = visible;
            MODIFIED.put(button, a);
        } else {
            if (a.fromCenter != null) button.setX(screenWidth / 2 + a.fromCenter);
            else if (button.getX() != a.x) button.setX(button.getX() + a.dx);
            if (button.getY() != a.y) button.setY(button.getY() + a.dy);
            if (button.getWidth() != a.w) button.setWidth(button.getWidth() + a.dw);
            if (a.visible != null) button.visible = a.visible;
        }
        a.x = button.getX();
        a.y = button.getY();
        a.w = button.getWidth();
    }

    public void renderExtras(GuiGraphicsExtractor poseStack, int mouseX, int mouseY, float partialTicks) {
        this.extraRenderables.forEach(r -> r.extractRenderState(poseStack, mouseX, mouseY, partialTicks));
    }
}
