package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.ISimpleExp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.Optional;

// x/y/width/height/z accept either a constant number or an expression (evaluated per-frame in render)
public record SimpleSprite(ResourceLocation texture, ISimpleExp x, ISimpleExp y, ISimpleExp width, ISimpleExp height,
                           ISimpleExp z, Optional<String> tooltip) implements Renderable {

    public static final Codec<SimpleSprite> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("texture").forGetter(SimpleSprite::texture),
            ISimpleExp.CODEC.fieldOf("x").forGetter(SimpleSprite::x),
            ISimpleExp.CODEC.fieldOf("y").forGetter(SimpleSprite::y),
            ISimpleExp.CODEC.fieldOf("width").forGetter(SimpleSprite::width),
            ISimpleExp.CODEC.fieldOf("height").forGetter(SimpleSprite::height),
            ISimpleExp.CODEC.optionalFieldOf("z", ISimpleExp.ZERO).forGetter(SimpleSprite::z),
            Codec.STRING.optionalFieldOf("tooltip").forGetter(SimpleSprite::tooltip)
    ).apply(i, SimpleSprite::new));


    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        float x = (float) this.x.evaluate();
        float y = (float) this.y.evaluate();
        float width = (float) this.width.evaluate();
        float height = (float) this.height.evaluate();
        float z = (float) this.z.evaluate();
        TextureAtlasSprite sprite = Minecraft.getInstance().getGuiSprites().getSprite(texture);
        blit(guiGraphics.pose().last().pose(), sprite.atlasLocation(), x, x + width, y, y + height, z,
                sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
    }


    //same as gui graphics inner blit
    public static void blit(Matrix4f matrix, ResourceLocation atlasLoc, float x1, float x2, float y1, float y2,
                            float blitOffset, float minU, float maxU, float minV, float maxV) {
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, atlasLoc);
        RenderSystem.enableBlend();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        bufferBuilder.addVertex(matrix, x1, y1, blitOffset).setUv(minU, minV);
        bufferBuilder.addVertex(matrix, x1, y2, blitOffset).setUv(minU, maxV);
        bufferBuilder.addVertex(matrix, x2, y2, blitOffset).setUv(maxU, maxV);
        bufferBuilder.addVertex(matrix, x2, y1, blitOffset).setUv(maxU, minV);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }
}
