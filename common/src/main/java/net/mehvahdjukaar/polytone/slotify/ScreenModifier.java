package net.mehvahdjukaar.polytone.slotify;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ScreenModifier(int titleX, int titleY, int labelX, int labelY,
                             List<SimpleSprite> sprites,
                             List<WidgetModifier> widgetModifiers,
                             Map<String, SpecialOffset> specialOffsets) {

    public ScreenModifier(GuiModifier original) {
        this(original.titleX(), original.titleY(), original.labelX(), original.labelY(), new ArrayList<>(original.sprites()),
                new ArrayList<>(original.widgetModifiers()),
                Map.copyOf(original.specialOffsets()));
    }

    public ScreenModifier merge(ScreenModifier other) {
        this.sprites.addAll(other.sprites);
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

    public void renderSprites(PoseStack poseStack) {
        RenderSystem.enableDepthTest();
        this.sprites.forEach(r -> r.render(poseStack));
    }
}
