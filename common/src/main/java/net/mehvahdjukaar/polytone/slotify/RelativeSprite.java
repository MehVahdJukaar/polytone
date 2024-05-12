package net.mehvahdjukaar.polytone.slotify;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public record RelativeSprite(ResourceLocation texture, int x, int y, int z, int width, int height) {


    public static final Codec<RelativeSprite> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("texture").forGetter(RelativeSprite::texture),
            Codec.INT.optionalFieldOf("x_inc", 0).forGetter(RelativeSprite::x),
            Codec.INT.optionalFieldOf("y_inc", 0).forGetter(RelativeSprite::y),
            Codec.INT.optionalFieldOf("z_inc", 0).forGetter(RelativeSprite::z),
            Codec.INT.optionalFieldOf("width_inc", 0).forGetter(RelativeSprite::width),
            Codec.INT.optionalFieldOf("height_inc", 0).forGetter(RelativeSprite::height)
    ).apply(i, RelativeSprite::new));


    public void render(PoseStack pose, int x1, int x2, int y1, int y2, int blitOffset) {
        blitOffset += z;

        int oldw = x2 - x1;
        x1 += x;
        oldw += width;
        x2 = x1 + oldw;

        int oldh = y2 - y1;
        y1 += y;
        oldh += height;
        y2 = y1 + oldh;
        TextureAtlasSprite sprite = Minecraft.getInstance().getGuiSprites().getSprite(texture);
        SimpleSprite.blit(pose.last().pose(), sprite.atlasLocation(), x1, x2, y1, y2, blitOffset,
                sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
    }
}
