package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.ISimpleExp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

// positional increments accept a constant number or an expression (evaluated per-blit)
public record RelativeSprite(ResourceLocation texture, ISimpleExp x, ISimpleExp y, ISimpleExp z, ISimpleExp width,
                             ISimpleExp height) {


    public static final Codec<RelativeSprite> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("texture").forGetter(RelativeSprite::texture),
            ISimpleExp.CODEC.optionalFieldOf("x_inc", ISimpleExp.ZERO).forGetter(RelativeSprite::x),
            ISimpleExp.CODEC.optionalFieldOf("y_inc", ISimpleExp.ZERO).forGetter(RelativeSprite::y),
            ISimpleExp.CODEC.optionalFieldOf("z_inc", ISimpleExp.ZERO).forGetter(RelativeSprite::z),
            ISimpleExp.CODEC.optionalFieldOf("width_inc", ISimpleExp.ZERO).forGetter(RelativeSprite::width),
            ISimpleExp.CODEC.optionalFieldOf("height_inc", ISimpleExp.ZERO).forGetter(RelativeSprite::height)
    ).apply(i, RelativeSprite::new));


    public void render(PoseStack pose, int x1, int x2, int y1, int y2, int blitOffset) {
        blitOffset += (int) z.evaluate();

        int oldw = x2 - x1;
        x1 += (int) x.evaluate();
        oldw += (int) width.evaluate();
        x2 = x1 + oldw;

        int oldh = y2 - y1;
        y1 += (int) y.evaluate();
        oldh += (int) height.evaluate();
        y2 = y1 + oldh;
        TextureAtlasSprite sprite = Minecraft.getInstance().getGuiSprites().getSprite(texture);
        SimpleSprite.blit(pose.last().pose(), sprite.atlasLocation(), x1, x2, y1, y2, blitOffset,
                sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
    }
}
