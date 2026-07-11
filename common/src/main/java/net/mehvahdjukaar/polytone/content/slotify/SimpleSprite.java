package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record SimpleSprite(Identifier texture, int x, int y, int width, int height,
                           Optional<GuiDepthTarget> depth,
                           Optional<String> tooltip) implements Renderable {//, Optional<ScreenSupplier> screenSupp) {

    public static final SchemaCodec<SimpleSprite> CODEC = SchemaRecord.create(SimpleSprite.class, i -> i.group(
            i.field("texture", Identifier.CODEC, SimpleSprite::texture),
            i.field("x", Codec.INT, SimpleSprite::x),
            i.field("y", Codec.INT, SimpleSprite::y),
            i.field("width", Codec.INT, SimpleSprite::width),
            i.field("height", Codec.INT, SimpleSprite::height),
            i.optional("depth", GuiDepthTarget.CODEC, SimpleSprite::depth),
            i.optional("tooltip", Codec.STRING, SimpleSprite::tooltip)
            // Codec.STRING.xmap(ScreenSupplier::decode, ScreenSupplier::toString).f
            //   .optionalFieldOf("screen_class").forGetter(SimpleSprite:: screenSupp)
    ).apply(i, SimpleSprite::new));


    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Runnable render = () ->
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, texture, x, y, width, height);
        GuiDepthTarget.renderAt(depth, guiGraphics, render);

    }

}
