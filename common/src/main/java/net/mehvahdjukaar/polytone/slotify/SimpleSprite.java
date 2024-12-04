package net.mehvahdjukaar.polytone.slotify;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.Optional;

public record SimpleSprite(ResourceLocation texture, float x, float y, float width, float height, float z,
                           Optional<String> tooltip) implements Renderable {//, Optional<ScreenSupplier> screenSupp) {

    public static final Codec<SimpleSprite> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("texture").forGetter(SimpleSprite::texture),
            Codec.FLOAT.fieldOf("x").forGetter(SimpleSprite::x),
            Codec.FLOAT.fieldOf("y").forGetter(SimpleSprite::y),
            Codec.FLOAT.fieldOf("width").forGetter(SimpleSprite::width),
            Codec.FLOAT.fieldOf("height").forGetter(SimpleSprite::height),
            Codec.FLOAT.optionalFieldOf("z", 0.0f).forGetter(SimpleSprite::z),
            Codec.STRING.optionalFieldOf("tooltip").forGetter(SimpleSprite::tooltip)
            // Codec.STRING.xmap(ScreenSupplier::decode, ScreenSupplier::toString).f
            //   .optionalFieldOf("screen_class").forGetter(SimpleSprite:: screenSupp)
    ).apply(i, SimpleSprite::new));


    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getGuiSprites().getSprite(texture);
        blit(guiGraphics.pose().last().pose(), sprite.atlasLocation(), x, x + width, y, y + height, z,
                sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
    }


    public static void blit(Matrix4f matrix, ResourceLocation atlasLoc, float x1, float x2, float y1, float y2,
                            float blitOffset, float minU, float maxU, float minV, float maxV) {
        RenderSystem.setShaderTexture(0, atlasLoc);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        ;
        bufferBuilder.addVertex(matrix, x1, y1, blitOffset).setUv(minU, minV);
        bufferBuilder.addVertex(matrix, x1, y2, blitOffset).setUv(minU, maxV);
        bufferBuilder.addVertex(matrix, x2, y2, blitOffset).setUv(maxU, maxV);
        bufferBuilder.addVertex(matrix, x2, y1, blitOffset).setUv(maxU, minV);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }
}
