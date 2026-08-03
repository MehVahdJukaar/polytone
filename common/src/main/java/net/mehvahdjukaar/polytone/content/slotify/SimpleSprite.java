package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Optional;

// x/y/width/height accept either a constant number or an expression (evaluated per-frame in render)
public record SimpleSprite(Identifier texture, ISimpleExp x, ISimpleExp y, ISimpleExp width, ISimpleExp height,
                           Optional<GuiDepthTarget> depth,
                           Optional<String> tooltip) implements Renderable {//, Optional<ScreenSupplier> screenSupp) {

    public static final SchemaCodec<SimpleSprite> CODEC = SchemaRecord.create(SimpleSprite.class, i -> i.group(
            i.field("texture", Identifier.CODEC, SimpleSprite::texture),
            i.field("x", ISimpleExp.CODEC, SimpleSprite::x),
            i.field("y", ISimpleExp.CODEC, SimpleSprite::y),
            i.field("width", ISimpleExp.CODEC, SimpleSprite::width),
            i.field("height", ISimpleExp.CODEC, SimpleSprite::height),
            i.optional("depth", GuiDepthTarget.CODEC, SimpleSprite::depth),
            i.optional("tooltip", Codec.STRING, SimpleSprite::tooltip)
            // Codec.STRING.xmap(ScreenSupplier::decode, ScreenSupplier::toString).f
            //   .optionalFieldOf("screen_class").forGetter(SimpleSprite:: screenSupp)
    ).apply(i, SimpleSprite::new));


    @Override
    public void extractRenderState(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        int x = (int) this.x.evaluate();
        int y = (int) this.y.evaluate();
        int width = (int) this.width.evaluate();
        int height = (int) this.height.evaluate();
        Runnable render = () ->
                GuiGraphicsExtractor.blitSprite(RenderPipelines.GUI_TEXTURED, texture, x, y, width, height);
        GuiDepthTarget.renderAt(depth, GuiGraphicsExtractor, render);

    }

}
