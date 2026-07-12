package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

// increments and UVs accept a constant number or an expression (evaluated per-blit). A UV of -1 means "keep original".
public record BlitModifier(TextureTarget target, int index, ISimpleExp xInc, ISimpleExp yInc, ISimpleExp widthInc,
                           ISimpleExp heightInc,
                           ISimpleExp u0, ISimpleExp v0, ISimpleExp u1, ISimpleExp v1, int color, Optional<Identifier> newTexture,
                           List<RelativeSprite> extraSprites) {

    private static final ISimpleExp MINUS_ONE = () -> -1.0;

    public static final SchemaCodec<BlitModifier> CODEC = SchemaRecord.create(BlitModifier.class, i -> i.group(
            i.field("texture", TextureTarget.CODEC, BlitModifier::target),
            i.optional("index", Codec.INT, -1, BlitModifier::index),
            i.optional("x_inc", ISimpleExp.CODEC, ISimpleExp.ZERO, BlitModifier::xInc),
            i.optional("y_inc", ISimpleExp.CODEC, ISimpleExp.ZERO, BlitModifier::yInc),
            i.optional("width_inc", ISimpleExp.CODEC, ISimpleExp.ZERO, BlitModifier::widthInc),
            i.optional("height_inc", ISimpleExp.CODEC, ISimpleExp.ZERO, BlitModifier::heightInc),
            i.optional("u0", ISimpleExp.CODEC, MINUS_ONE, BlitModifier::u0),
            i.optional("v0", ISimpleExp.CODEC, MINUS_ONE, BlitModifier::v0),
            i.optional("u1", ISimpleExp.CODEC, MINUS_ONE, BlitModifier::u1),
            i.optional("v1", ISimpleExp.CODEC, MINUS_ONE, BlitModifier::v1),
            i.optional("color", ColorUtils.COLOR, -1, BlitModifier::color),
            i.optional("new_texture", Identifier.CODEC, BlitModifier::newTexture),
            i.optional("overlays", RelativeSprite.CODEC.listOf(), List.of(), BlitModifier::extraSprites)
    ).apply(i, BlitModifier::new));


    public void blitModified(GuiGraphics gui, RenderPipeline pipeline,
                             TextureAtlasSprite sprite,
                             int oldX1, int oldX2, int oldY1, int oldY2,
                             float oldU0, float oldU1, float oldV0, float oldV1,
                             int tint) {

        int col = this.color == -1 ? tint : color;


        for (RelativeSprite s : extraSprites) {
            s.render(gui, pipeline, oldX1, oldX2, oldY1, oldY2, col);
        }

        if (newTexture.isPresent()) {
            var material = new Material(Sheets.GUI_SHEET, newTexture.get());
            sprite = gui.getSprite(material);
        }
        float u0 = (float) this.u0.evaluate();
        float u1 = (float) this.u1.evaluate();
        float v0 = (float) this.v0.evaluate();
        float v1 = (float) this.v1.evaluate();
        float minU = u0 == -1 ? oldU0 : u0;
        float maxU = u1 == -1 ? oldU1 : u1;
        float minV = v0 == -1 ? oldV0 : v0;
        float maxV = v1 == -1 ? oldV1 : v1;

        int oldw = oldX2 - oldX1;
        oldX1 += (int) xInc.evaluate();
        oldw += (int) widthInc.evaluate();
        oldX2 = oldX1 + oldw;

        int oldh = oldY2 - oldY1;
        oldY1 += (int) yInc.evaluate();
        oldh += (int) heightInc.evaluate();
        oldY2 = oldY1 + oldh;

        gui.innerBlit(pipeline, sprite.atlasLocation(), oldX1, oldX2, oldY1, oldY2, minU, maxU, minV, maxV, col);
    }


}
