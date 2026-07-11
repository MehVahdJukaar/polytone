package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
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

    public static final SchemaCodec<SimpleText> CODEC = SchemaRecord.create(SimpleText.class, i -> i.group(
            i.field("text", ComponentSerialization.CODEC, SimpleText::text),
            i.field("x", Codec.INT, SimpleText::x),
            i.field("y", Codec.INT, SimpleText::y),
            i.optional("depth", GuiDepthTarget.CODEC, SimpleText::depth),
            i.optional("color", ColorUtils.COLOR, -1, SimpleText::color),
            i.optional("centered", Codec.BOOL, false, SimpleText::centered)
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
