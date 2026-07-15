package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.ISimpleExp;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

// x/y/z accept a constant number or an expression; color stays a packed ARGB int (doesn't interpolate as a double)
public record SimpleText(Component text, ISimpleExp x, ISimpleExp y, ISimpleExp z,
                         int color, boolean centered) implements Renderable {

    public static final Codec<SimpleText> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("text").forGetter(SimpleText::text),
            ISimpleExp.CODEC.fieldOf("x").forGetter(SimpleText::x),
            ISimpleExp.CODEC.fieldOf("y").forGetter(SimpleText::y),
            ISimpleExp.CODEC.optionalFieldOf("z", ISimpleExp.ZERO).forGetter(SimpleText::z),
            ColorUtils.CODEC.optionalFieldOf("color", -1).forGetter(SimpleText::color),
            Codec.BOOL.optionalFieldOf("centered", false).forGetter(SimpleText::centered)
    ).apply(i, SimpleText::new));

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int x = (int) this.x.evaluate();
        int y = (int) this.y.evaluate();
        int z = (int) this.z.evaluate();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0,0, z);
        if (centered) {
            guiGraphics.drawCenteredString(font, text, x, y, color);
        }
        else guiGraphics.drawString(font, text, x, y, color);
        guiGraphics.pose().popPose();
    }
}
