package net.mehvahdjukaar.polytone.slotify;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.Optional;

public record BlitModifier(ResourceLocation target, int index, int x, int y, int width, int height,
                           float u1, float v1, float u2, float v2, Optional<ResourceLocation> newTexture) {

    public static final Codec<BlitModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("texture").forGetter(BlitModifier::target),
            StrOpt.of(Codec.INT, "index", -1).forGetter(BlitModifier::index),
            StrOpt.of(Codec.INT, "x_inc", 0).forGetter(BlitModifier::x),
            StrOpt.of(Codec.INT, "y_inc", 0).forGetter(BlitModifier::y),
            StrOpt.of(Codec.INT, "width_inc", 0).forGetter(BlitModifier::width),
            StrOpt.of(Codec.INT, "height_inc", 0).forGetter(BlitModifier::height),
            StrOpt.of(Codec.FLOAT, "u0", -1f).forGetter(BlitModifier::u1),
            StrOpt.of(Codec.FLOAT, "v0", -1f).forGetter(BlitModifier::v1),
            StrOpt.of(Codec.FLOAT, "u1", -1f).forGetter(BlitModifier::u2),
            StrOpt.of(Codec.FLOAT, "v1", -1f).forGetter(BlitModifier::v2),
            StrOpt.of(ResourceLocation.CODEC, "new_texture").forGetter(BlitModifier::newTexture)
    ).apply(i, BlitModifier::new));


    public void blitModified(GuiGraphics gui, ResourceLocation texture, int x1, int x2, int y1, int y2, int blitOffset,
                             float minU, float maxU, float minV, float maxV) {

        if (newTexture.isPresent()) texture = newTexture.get();
        x1 += x;
        int oldw = x2 - x1;
        oldw += width;
        x2 = x1 + oldw;

        y1 += y;
        int oldh = y2 - y1;
        oldh += height;
        y2 = y1 + oldh;


        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = gui.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix4f, (float) x1, (float) y1, (float) blitOffset).uv(minU, minV).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x1, (float) y2, (float) blitOffset).uv(minU, maxV).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x2, (float) y2, (float) blitOffset).uv(maxU, maxV).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x2, (float) y1, (float) blitOffset).uv(maxU, minV).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }
}
