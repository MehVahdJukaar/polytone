package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

// increments and UVs accept a constant number or an expression (evaluated per-blit). A UV of -1 means "keep original".
public record BlitModifier(ResourceLocation target, int index, ISimpleExp xInc, ISimpleExp yInc, ISimpleExp zInc,
                           ISimpleExp widthInc, ISimpleExp heightInc,
                           ISimpleExp u0, ISimpleExp v0, ISimpleExp u1, ISimpleExp v1, Optional<ResourceLocation> newTexture,
                           List<RelativeSprite> extraSprites) {

    private static final ISimpleExp MINUS_ONE = () -> -1.0;

    public static final Codec<BlitModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("texture").forGetter(BlitModifier::target),
            Codec.INT.optionalFieldOf("index", -1).forGetter(BlitModifier::index),
            ISimpleExp.CODEC.optionalFieldOf("x_inc", ISimpleExp.ZERO).forGetter(BlitModifier::xInc),
            ISimpleExp.CODEC.optionalFieldOf("y_inc", ISimpleExp.ZERO).forGetter(BlitModifier::yInc),
            ISimpleExp.CODEC.optionalFieldOf("z_inc", ISimpleExp.ZERO).forGetter(BlitModifier::zInc),
            ISimpleExp.CODEC.optionalFieldOf("width_inc", ISimpleExp.ZERO).forGetter(BlitModifier::widthInc),
            ISimpleExp.CODEC.optionalFieldOf("height_inc", ISimpleExp.ZERO).forGetter(BlitModifier::heightInc),
            ISimpleExp.CODEC.optionalFieldOf("u0", MINUS_ONE).forGetter(BlitModifier::u0),
            ISimpleExp.CODEC.optionalFieldOf("v0", MINUS_ONE).forGetter(BlitModifier::v0),
            ISimpleExp.CODEC.optionalFieldOf("u1", MINUS_ONE).forGetter(BlitModifier::u1),
            ISimpleExp.CODEC.optionalFieldOf("v1", MINUS_ONE).forGetter(BlitModifier::v1),
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
        float u0 = (float) this.u0.evaluate();
        float u1 = (float) this.u1.evaluate();
        float v0 = (float) this.v0.evaluate();
        float v1 = (float) this.v1.evaluate();
        float minU = u0 == -1 ? oldU0 : u0;
        float maxU = u1 == -1 ? oldU1 : u1;
        float minV = v0 == -1 ? oldV0 : v0;
        float maxV = v1 == -1 ? oldV1 : v1;

        blitOffset += (int) zInc.evaluate();

        int oldw = oldX2 - oldX1;
        oldX1 += (int) xInc.evaluate();
        oldw += (int) widthInc.evaluate();
        oldX2 = oldX1 + oldw;

        int oldh = oldY2 - oldY1;
        oldY1 += (int) yInc.evaluate();
        oldh += (int) heightInc.evaluate();
        oldY2 = oldY1 + oldh;


        SimpleSprite.blit(gui.pose().last().pose(), sprite.atlasLocation(),
                (float) oldX1, (float) oldX2, (float) oldY1, (float) oldY2, (float) blitOffset, minU, maxU, minV, maxV);
    }


}
