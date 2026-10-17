package net.mehvahdjukaar.polytone.content.lightmap;

import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

public class GuiLightmap {

    public static final Identifier TEXTURE = Polytone.res("lightmaps/gui.png");

    private boolean renderingGui = false;

    public void setRenderingGui(boolean gui) {
        renderingGui = gui;
    }

    public boolean isActive() {
        return renderingGui && PlatStuff.isModStateValid();
    }

    public AbstractTexture getTexture() {
        return Minecraft.getInstance().getTextureManager().getTexture(TEXTURE);
    }
}
