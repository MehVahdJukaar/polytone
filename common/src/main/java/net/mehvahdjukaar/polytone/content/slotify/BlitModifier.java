package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public record BlitModifier(TextureTarget target, int index, int xInc, int yInc,   int widthInc,
                           int heightInc,
                           float u0, float v0, float u1, float v1, int color, Optional<Identifier> newTexture,
                           List<RelativeSprite> extraSprites) {

    public static final SchemaCodec<BlitModifier> CODEC = SchemaRecord.create(BlitModifier.class, i -> i.group(
            i.field("texture", TextureTarget.CODEC, BlitModifier::target),
            i.optional("index", Codec.INT, -1, BlitModifier::index),
            i.optional("x_inc", Codec.INT, 0, BlitModifier::xInc),
            i.optional("y_inc", Codec.INT, 0, BlitModifier::yInc),
            i.optional("width_inc", Codec.INT, 0, BlitModifier::widthInc),
            i.optional("height_inc", Codec.INT, 0, BlitModifier::heightInc),
            i.optional("u0", Codec.FLOAT, -1f, BlitModifier::u0),
            i.optional("v0", Codec.FLOAT, -1f, BlitModifier::v0),
            i.optional("u1", Codec.FLOAT, -1f, BlitModifier::u1),
            i.optional("v1", Codec.FLOAT, -1f, BlitModifier::v1),
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
        float minU = u0 == -1 ? oldU0 : u0;
        float maxU = u1 == -1 ? oldU1 : u1;
        float minV = v0 == -1 ? oldV0 : v0;
        float maxV = v1 == -1 ? oldV1 : v1;

        int oldw = oldX2 - oldX1;
        oldX1 += xInc;
        oldw += widthInc;
        oldX2 = oldX1 + oldw;

        int oldh = oldY2 - oldY1;
        oldY1 += yInc;
        oldh += heightInc;
        oldY2 = oldY1 + oldh;

        gui.innerBlit(pipeline, sprite.atlasLocation(), oldX1, oldX2, oldY1, oldY2, minU, maxU, minV, maxV, col);
    }


}
