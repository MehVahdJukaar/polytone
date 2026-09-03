package net.mehvahdjukaar.polytone.content.config;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.nautilus.PolytoneNautilus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BooleanSupplier;

class EditorIconButton extends SpriteIconButton.CenteredIcon {
    private static final ResourceLocation SPRITE = Polytone.res("codec_editor");
    private static final ResourceLocation SPRITE_ON = Polytone.res("codec_editor_on");
    private static final ResourceLocation SPRITE_LOADING = Polytone.res("codec_editor_loading");

    private final boolean available;
    private final BooleanSupplier loading;

    EditorIconButton(int width, int height, Component message, boolean available,
                     BooleanSupplier booting, OnPress onPress) {
        super(width, height, message, 16, 16, SPRITE, onPress, null);
        this.available = available;
        this.loading = booting;
    }

    private ResourceLocation icon() {
        if (!available) return SPRITE;
        if (loading.getAsBoolean()) return SPRITE_LOADING;
        return PolytoneNautilus.isOpen() ? SPRITE_ON : SPRITE;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.sprite = icon();
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }
}
