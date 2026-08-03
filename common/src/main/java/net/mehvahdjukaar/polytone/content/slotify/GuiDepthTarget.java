package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ExtraCodecs;

import java.util.Optional;

public record GuiDepthTarget(int strata, int node, boolean addAbove) {

    public static final SchemaCodec<GuiDepthTarget> CODEC = SchemaRecord.create(GuiDepthTarget.class, i -> i.group(
            i.field("strata", ExtraCodecs.NON_NEGATIVE_INT, GuiDepthTarget::strata),
            i.optional("node", ExtraCodecs.NON_NEGATIVE_INT, Integer.MAX_VALUE, GuiDepthTarget::node),
            i.optional("add_above", Codec.BOOL, true, GuiDepthTarget::addAbove)
    ).apply(i, GuiDepthTarget::new));

    public static void renderAt(Optional<GuiDepthTarget> depth, GuiGraphicsExtractor GuiGraphicsExtractor, Runnable render) {
        if (depth.isPresent()) {
            ((GuiDepthTargetAware) GuiGraphicsExtractor).polytone$renderInNode(depth.get(), render);
        } else {
            render.run();
        }
    }
}
