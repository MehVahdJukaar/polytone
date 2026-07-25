package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.Optional;

// x/y accept a constant number or an expression (evaluated per-frame in render); color stays a packed ARGB int
public record SimpleText(Component text, ISimpleExp x, ISimpleExp y, Optional<GuiDepthTarget> depth,
                         int color, boolean centered) implements Renderable {

    public static final SchemaCodec<SimpleText> CODEC = SchemaRecord.create(SimpleText.class, i -> i.group(
            i.field("text", ComponentSerialization.CODEC, SimpleText::text),
            i.field("x", ISimpleExp.CODEC, SimpleText::x),
            i.field("y", ISimpleExp.CODEC, SimpleText::y),
            i.optional("depth", GuiDepthTarget.CODEC, SimpleText::depth),
            i.optional("color", ColorUtils.COLOR, -1, SimpleText::color),
            i.optional("centered", Codec.BOOL, false, SimpleText::centered)
    ).apply(i, SimpleText::new));

    @Override
    public void extractRenderState(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int x = (int) this.x.evaluate();
        int y = (int) this.y.evaluate();

        GuiDepthTarget.renderAt(depth, GuiGraphicsExtractor, () -> {
            GuiGraphicsExtractor.pose().pushMatrix();
            if (centered) {
                GuiGraphicsExtractor.centeredText(font, text, x, y, color);
            } else GuiGraphicsExtractor.text(font, text, x, y, color);
            GuiGraphicsExtractor.pose().popMatrix();
        });

    }

}
