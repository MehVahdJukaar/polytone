package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.resources.Identifier;

public interface GuiDepthTargetAware {

    void polytone$renderInNode(GuiDepthTarget nodeTarget, Runnable renderFunction);

    default void polytone$innerBlit(RenderPipeline pipeline, Identifier location,
                                    int x0, int x1, int y0, int y1,
                                    float u0, float u1, float v0, float v1, int color) {}
}
