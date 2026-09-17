package net.mehvahdjukaar.polytone.content.config;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.nautilus.PolytoneNautilus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

final class EditorButton extends Button {

    private static final Identifier ICON = Polytone.res("codec_editor");
    private static final Identifier ICON_ACTIVE = Polytone.res("codec_editor_on");
    private static final Identifier ICON_LOADING = Polytone.res("codec_editor_loading");
    private static final String NAUTILUS_URL = "https://github.com/MehVahdJukaar/pack_editor";

    private final int spriteWidth;
    private final int spriteHeight;
    private final boolean available;
    private volatile boolean loading;

    EditorButton(int size, int spriteWidth, int spriteHeight, boolean available, Component tooltip) {
        super(0, 0, size, size, Component.empty(), b -> {}, DEFAULT_NARRATION);
        this.spriteWidth = spriteWidth;
        this.spriteHeight = spriteHeight;
        this.available = available;
        setTooltip(Tooltip.create(tooltip));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (!available) {
            openDownloadPage();
            return;
        }
        open();
    }

    private void openDownloadPage() {
        Polytone.CONFIGS.bubbleManager.onEditorButtonClicked();
        Minecraft mc = Minecraft.getInstance();
        Screen parent = mc.screen;
        mc.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) Util.getPlatform().openUri(NAUTILUS_URL);
            mc.setScreen(parent);
        }, NAUTILUS_URL, true));
    }

    private void open() {
        if (loading || Minecraft.getInstance().level == null) return;
        if (PolytoneNautilus.isOpen()) {
            PolytoneNautilus.open();
            return;
        }
        loading = true;
        Thread t = new Thread(() -> {
            try {
                PolytoneNautilus.open();
            } catch (Throwable e) {
                Polytone.LOGGER.error("Failed to open Polytone codec editor", e);
            } finally {
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> loading = false);
            }
        }, "Polytone-Editor-Boot");
        t.setDaemon(true);
        t.start();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        extractDefaultSprite(guiGraphics);
        boolean active = available && !loading && PolytoneNautilus.isOpen();
        Identifier sprite = loading ? ICON_LOADING : (active ? ICON_ACTIVE : ICON);
        int x = getX() + (getWidth() - spriteWidth) / 2;
        int y = getY() + (getHeight() - spriteHeight) / 2;
        float a = this.alpha * (isActive() ? 1f : 0.4f);

        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, spriteWidth, spriteHeight, a);
    }
}
