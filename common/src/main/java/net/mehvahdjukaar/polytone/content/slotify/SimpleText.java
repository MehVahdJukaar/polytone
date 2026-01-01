package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.Optional;

public record SimpleText(Component text, int x, int y, Optional<GuiDepthTarget> depth,
                         int color, boolean centered) implements Renderable {

    public static final Codec<SimpleText> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("text").forGetter(SimpleText::text),
            Codec.INT.fieldOf("x").forGetter(SimpleText::x),
            Codec.INT.fieldOf("y").forGetter(SimpleText::y),
            GuiDepthTarget.CODEC.optionalFieldOf("depth").forGetter(SimpleText::depth),
            ColorUtils.COLOR.optionalFieldOf("color", -1).forGetter(SimpleText::color),
            Codec.BOOL.optionalFieldOf("centered", false).forGetter(SimpleText::centered)
    ).apply(i, SimpleText::new));

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;

        GuiDepthTarget.renderAt(depth, guiGraphics, () -> {
            guiGraphics.pose().pushMatrix();
            if (centered) {
                guiGraphics.drawCenteredString(font, text, x, y, color);
            } else guiGraphics.drawString(font, text, x, y, color);
            guiGraphics.pose().popMatrix();
        });

    }

}
