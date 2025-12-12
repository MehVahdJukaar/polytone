package net.mehvahdjukaar.polytone.slotify;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.GuiDepthTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record RelativeSprite(Identifier texture, int x, int y, Optional<GuiDepthTarget> depth, int width,
                             int height) {


    public static final Codec<RelativeSprite> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("texture").forGetter(RelativeSprite::texture),
            Codec.INT.optionalFieldOf("x_inc", 0).forGetter(RelativeSprite::x),
            Codec.INT.optionalFieldOf("y_inc", 0).forGetter(RelativeSprite::y),
            GuiDepthTarget.CODEC.optionalFieldOf("depth").forGetter(RelativeSprite::depth),
            Codec.INT.optionalFieldOf("width_inc", 0).forGetter(RelativeSprite::width),
            Codec.INT.optionalFieldOf("height_inc", 0).forGetter(RelativeSprite::height)
    ).apply(i, RelativeSprite::new));


    public void render(GuiGraphics graphics, RenderPipeline pipeline,
                       int x1, int x2, int y1, int y2, int color) {

        int oldw = x2 - x1;
        x1 += x;
        oldw += width;
        x2 = x1 + oldw;

        int oldh = y2 - y1;
        y1 += y;
        oldh += height;
        y2 = y1 + oldh;
        var material = new Material(Sheets.GUI_SHEET, texture);
        TextureAtlasSprite sprite = graphics.getSprite(material);

        int finalX = x1;
        int finalX1 = x2;
        int finalY = y1;
        int finalY1 = y2;
        GuiDepthTarget.renderAt(depth, graphics, () -> {
            graphics.innerBlit(pipeline, texture, finalX, finalX1, finalY, finalY1,
                    sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), color);
        });
    }
}
