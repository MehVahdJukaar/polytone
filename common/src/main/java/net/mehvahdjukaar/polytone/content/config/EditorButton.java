package net.mehvahdjukaar.polytone.content.config;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.PolytoneEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The "open codec editor" button on the {@link ConfigScreen} footer. It boots the Swing
 * workbench, which is heavy (schema bootstrap + window build), so the open runs on a
 * background thread and the button shows an animated spinner sprite meanwhile. The static
 * icon and the spinner are ordinary GUI sprites; the spinner's {@code .mcmeta} makes the
 * atlas tick it, so no manual frame stepping is needed here.
 */
final class EditorButton extends Button {

    private static final Identifier ICON = Polytone.res("codec_editor");
    private static final Identifier ICON_ACTIVE = Polytone.res("codec_editor_on");
    private static final Identifier ICON_LOADING = Polytone.res("codec_editor_loading");
    private static final int DOT_SIZE = 6;

    private final int spriteWidth;
    private final int spriteHeight;
    /** Whether the separate PackEditor mod is installed. When false, every {@link PolytoneEditor}
     *  call is short-circuited so its (absent) classes are never loaded, and the button stays grey. */
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
        open();
    }

    /** Boot the editor off-thread so the spinner keeps animating; re-enabled when it returns. */
    private void open() {
        if (!available || loading || Minecraft.getInstance().level == null) return;
        // Already open: just focus it — no spinner, no rebuild (single instance).
        if (PolytoneEditor.isOpen()) {
            PolytoneEditor.open();
            return;
        }
        loading = true;
        Thread t = new Thread(() -> {
            try {
                PolytoneEditor.open();
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
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Vanilla button background (greys to the disabled sprite when inactive) + centred icon.
        renderDefaultSprite(guiGraphics);
        boolean active = available && !loading && PolytoneEditor.isOpen();
        Identifier sprite = loading ? ICON_LOADING : (active ? ICON_ACTIVE : ICON);
        int x = getX() + (getWidth() - spriteWidth) / 2;
        int y = getY() + (getHeight() - spriteHeight) / 2;
        float a = this.alpha * (isActive() ? 1f : 0.4f); // dim the glyph too while greyed out

        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, spriteWidth, spriteHeight, a);
    }
}
