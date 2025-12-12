package net.mehvahdjukaar.polytone.slotify;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.GuiDepthTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record SimpleSprite(Identifier texture, int x, int y, int width, int height,
                           Optional<GuiDepthTarget> depth,
                           Optional<String> tooltip) implements Renderable {//, Optional<ScreenSupplier> screenSupp) {

    public static final Codec<SimpleSprite> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("texture").forGetter(SimpleSprite::texture),
            Codec.INT.fieldOf("x").forGetter(SimpleSprite::x),
            Codec.INT.fieldOf("y").forGetter(SimpleSprite::y),
            Codec.INT.fieldOf("width").forGetter(SimpleSprite::width),
            Codec.INT.fieldOf("height").forGetter(SimpleSprite::height),
            GuiDepthTarget.CODEC.optionalFieldOf("depth").forGetter(SimpleSprite::depth),
            Codec.STRING.optionalFieldOf("tooltip").forGetter(SimpleSprite::tooltip)
            // Codec.STRING.xmap(ScreenSupplier::decode, ScreenSupplier::toString).f
            //   .optionalFieldOf("screen_class").forGetter(SimpleSprite:: screenSupp)
    ).apply(i, SimpleSprite::new));


    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        var material = new Material(Sheets.GUI_SHEET, texture);
        TextureAtlasSprite sprite = guiGraphics.getSprite(material);
        Runnable render = () -> {
            guiGraphics.blit(texture,
                    x, x + width, y, y + height,
                    sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
        };
        GuiDepthTarget.renderAt(depth, guiGraphics, render);

    }

}
