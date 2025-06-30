package net.mehvahdjukaar.polytone.slotify;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record BlitModifier(ResourceLocation target, int index, int xInc, int yInc, int zInc, int widthInc, int heightInc,
                           float u0, float v0, float u1, float v1, Optional<ResourceLocation> newTexture,
                           List<RelativeSprite> extraSprites) {

    public static final Codec<BlitModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("texture").forGetter(BlitModifier::target),
            Codec.INT.optionalFieldOf("index", -1).forGetter(BlitModifier::index),
            Codec.INT.optionalFieldOf("x_inc", 0).forGetter(BlitModifier::xInc),
            Codec.INT.optionalFieldOf("y_inc", 0).forGetter(BlitModifier::yInc),
            Codec.INT.optionalFieldOf("z_inc", 0).forGetter(BlitModifier::zInc),
            Codec.INT.optionalFieldOf("width_inc", 0).forGetter(BlitModifier::widthInc),
            Codec.INT.optionalFieldOf("height_inc", 0).forGetter(BlitModifier::heightInc),
            Codec.FLOAT.optionalFieldOf("u0", -1f).forGetter(BlitModifier::u0),
            Codec.FLOAT.optionalFieldOf("v0", -1f).forGetter(BlitModifier::v0),
            Codec.FLOAT.optionalFieldOf("u1", -1f).forGetter(BlitModifier::u1),
            Codec.FLOAT.optionalFieldOf("v1", -1f).forGetter(BlitModifier::v1),
            ResourceLocation.CODEC.optionalFieldOf("new_texture").forGetter(BlitModifier::newTexture),
            RelativeSprite.CODEC.listOf().optionalFieldOf("overlays", List.of()).forGetter(BlitModifier::extraSprites)
    ).apply(i, BlitModifier::new));


    public void blitModified(GuiGraphics gui, TextureAtlasSprite sprite,
                             int oldX1,  int oldX2, int oldY1,int oldY2, int blitOffset,
                             float oldU0, float oldU1, float oldV0, float oldV1) {

        for (RelativeSprite s : extraSprites) {
            s.render(gui.pose(), oldX1, oldX2, oldY1, oldY2, blitOffset);
        }

        if (newTexture.isPresent()) {
            sprite = Minecraft.getInstance().getGuiSprites().getSprite(newTexture.get());
        }
        float minU = u0 == -1 ? oldU0 : u0;
        float maxU = u1 == -1 ? oldU1 : u1;
        float minV = v0 == -1 ? oldV0 : v0;
        float maxV = v1 == -1 ? oldV1 : v1;

        blitOffset += zInc;

        int oldw = oldX2 - oldX1;
        oldX1 += xInc;
        oldw += widthInc;
        oldX2 = oldX1 + oldw;

        int oldh = oldY2 - oldY1;
        oldY1 += yInc;
        oldh += heightInc;
        oldY2 = oldY1 + oldh;


        SimpleSprite.blit(gui.pose().last().pose(), sprite.atlasLocation(),
                (float) oldX1, (float) oldX2, (float) oldY1, (float) oldY2, (float) blitOffset, minU, maxU, minV, maxV);
    }


}
