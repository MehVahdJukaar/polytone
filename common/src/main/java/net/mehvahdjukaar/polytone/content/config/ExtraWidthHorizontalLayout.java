package net.mehvahdjukaar.polytone.content.config;

import net.minecraft.client.gui.layouts.LinearLayout;

public class ExtraWidthHorizontalLayout extends LinearLayout {

    public final int extraWidth;
    public final int extraX;

    public ExtraWidthHorizontalLayout(int extraWidth, int extraX) {
        super(0,0, Orientation.HORIZONTAL);
        this.extraWidth = extraWidth;
        this.extraX = extraX;
    }

    @Override
    public int getX() {
        return super.getX() + extraX;
    }

    @Override
    public int getWidth() {
        return super.getWidth() + extraWidth;
    }
}
