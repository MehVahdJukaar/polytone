package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;

import java.util.Optional;

// positional increments accept a constant number or an expression (evaluated per-blit)
public record RelativeSprite(Identifier texture, ISimpleExp x, ISimpleExp y, Optional<GuiDepthTarget> depth,
                             ISimpleExp width, ISimpleExp height) {


    public static final SchemaCodec<RelativeSprite> CODEC = SchemaRecord.create(RelativeSprite.class, i -> i.group(
            i.field("texture", Identifier.CODEC, RelativeSprite::texture),
            i.optional("x_inc", ISimpleExp.CODEC, ISimpleExp.ZERO, RelativeSprite::x),
            i.optional("y_inc", ISimpleExp.CODEC, ISimpleExp.ZERO, RelativeSprite::y),
            i.optional("depth", GuiDepthTarget.CODEC, RelativeSprite::depth),
            i.optional("width_inc", ISimpleExp.CODEC, ISimpleExp.ZERO, RelativeSprite::width),
            i.optional("height_inc", ISimpleExp.CODEC, ISimpleExp.ZERO, RelativeSprite::height)
    ).apply(i, RelativeSprite::new));


    public void render(GuiGraphics graphics, RenderPipeline pipeline,
                       int x1, int x2, int y1, int y2, int color) {

        int oldw = x2 - x1;
        x1 += (int) x.evaluate();
        oldw += (int) width.evaluate();
        x2 = x1 + oldw;

        int oldh = y2 - y1;
        y1 += (int) y.evaluate();
        oldh += (int) height.evaluate();
        y2 = y1 + oldh;
        var material = new Material(Sheets.GUI_SHEET, texture);
        TextureAtlasSprite sprite = graphics.getSprite(material);

        int finalX = x1;
        int finalX1 = x2;
        int finalY = y1;
        int finalY1 = y2;
        GuiDepthTarget.renderAt(depth, graphics, () ->
                graphics.innerBlit(pipeline, texture, finalX, finalX1, finalY, finalY1,
                sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), color));
    }
}
