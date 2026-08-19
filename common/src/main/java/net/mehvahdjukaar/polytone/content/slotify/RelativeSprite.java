package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
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


    public void render(GuiGraphicsExtractor graphics, RenderPipeline pipeline,
                       int x1, int x2, int y1, int y2, int color) {

        int oldw = x2 - x1;
        x1 += (int) x.evaluate();
        oldw += (int) width.evaluate();
        x2 = x1 + oldw;

        int oldh = y2 - y1;
        y1 += (int) y.evaluate();
        oldh += (int) height.evaluate();
        y2 = y1 + oldh;
        TextureAtlasSprite sprite = graphics.getSprite(new SpriteId(Sheets.GUI_SHEET, texture));

        int finalX = x1;
        int finalX1 = x2;
        int finalY = y1;
        int finalY1 = y2;
        GuiDepthTarget.renderAt(depth, graphics, () ->
                graphics.blitSprite(pipeline, sprite, finalX, finalY, finalX1 - finalX, finalY1 - finalY, color));
    }
}
