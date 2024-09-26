package net.mehvahdjukaar.polytone.slotify;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ScreenModifier(int titleX, int titleY, int labelX, int labelY,
                             @Nullable Integer titleColor, @Nullable Integer labelColor,
                             List<Renderable> extraRenderables,
                             List<WidgetModifier> widgetModifiers,
                             Map<String, SpecialOffset> specialOffsets) {

    public static ScreenModifier fromGuiMod(GuiModifier original) {
        List<Renderable> lis = new ArrayList<>(original.sprites());
        lis.addAll(original.textList());
        return new ScreenModifier(original.titleX(), original.titleY(), original.labelX(), original.labelY(),
                original.titleColor(), original.labelColor(),
                lis,
                new ArrayList<>(original.widgetModifiers()),
                Map.copyOf(original.specialOffsets()));
    }

    public ScreenModifier merge(ScreenModifier other) {
        this.extraRenderables.addAll(other.extraRenderables);
        this.specialOffsets.putAll(other.specialOffsets);
        return this;
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
