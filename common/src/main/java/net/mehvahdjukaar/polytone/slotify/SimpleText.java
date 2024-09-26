package net.mehvahdjukaar.polytone.slotify;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public record SimpleText(Component text, int x, int y, int z,
                         int color, boolean centered) implements Renderable {

    public static final Codec<SimpleText> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("text").forGetter(SimpleText::text),
            Codec.INT.fieldOf("x").forGetter(SimpleText::x),
            Codec.INT.fieldOf("y").forGetter(SimpleText::y),
            Codec.INT.optionalFieldOf("z", 0).forGetter(SimpleText::z),
            ColorUtils.CODEC.optionalFieldOf("color", -1).forGetter(SimpleText::color),
            Codec.BOOL.optionalFieldOf("centered", false).forGetter(SimpleText::centered)
    ).apply(i, SimpleText::new));

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0,0, z);
        if (centered) {
            guiGraphics.drawCenteredString(font, text, x, y, color);
        }
        else guiGraphics.drawString(font, text, x, y, color);
        guiGraphics.pose().popPose();
    }
}
