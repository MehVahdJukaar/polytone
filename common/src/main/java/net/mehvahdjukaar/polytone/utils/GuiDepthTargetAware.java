package net.mehvahdjukaar.polytone.utils;

public interface GuiDepthTargetAware {

    void renderInNode(GuiDepthTarget nodeTarget, Runnable renderFunction);
}
