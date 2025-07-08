package net.mehvahdjukaar.polytone.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.ExtraCodecs;

import java.util.Optional;

public record GuiDepthTarget(int strata, int node, boolean addAbove) {

    public static final Codec<GuiDepthTarget> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("strata").forGetter(GuiDepthTarget::strata),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("node", Integer.MAX_VALUE).forGetter(GuiDepthTarget::node),
            Codec.BOOL.optionalFieldOf("add_above", true).forGetter(GuiDepthTarget::addAbove)
    ).apply(i, GuiDepthTarget::new));

    public static void renderAt(Optional<GuiDepthTarget> depth, GuiGraphics guiGraphics, Runnable render) {
        if (depth.isPresent()) {
            ((GuiDepthTargetAware) guiGraphics).renderInNode(depth.get(), render);
        } else {
            render.run();
        }
    }
}
