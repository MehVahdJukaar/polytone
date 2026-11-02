package net.mehvahdjukaar.polytone.content.config;

import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

//copied from Moonlight class. Keep in sync with that one
public class ColorPickerScreen extends Screen {

    private static final int GAP = 4;
    private static final int CONTROL_HEIGHT = Button.DEFAULT_HEIGHT;
    private static final int HEADER = 44;

    private final Screen parent;
    private final Consumer<Integer> onApply;
    private final boolean hasAlpha;

    private float hue, sat, val, alpha;

    private int svX, svY, svSize;
    private int hueX, hueY, hueW, hueH;
    private int alphaX, alphaY, alphaW, alphaH;

    private ColorFieldWidget control;
    private boolean suppressControlSync;
    private Drag dragging = Drag.NONE;

    private enum Drag {NONE, SV, HUE, ALPHA}

    public ColorPickerScreen(int color, Screen parent, Consumer<Integer> onApply) {
        this(color, true, parent, onApply);
    }

    public ColorPickerScreen(int color, boolean hasAlpha, Screen parent, Consumer<Integer> onApply) {
        super(Component.translatable("gui.polytone.color_picker"));
        this.parent = parent;
        this.onApply = onApply;
        this.hasAlpha = hasAlpha;
        float[] hsv = ColorUtils.argbToHsv(color);
        this.hue = hsv[0];
        this.sat = hsv[1];
        this.val = hsv[2];
        this.alpha = hasAlpha ? FastColor.ARGB32.alpha(color) / 255f : 1;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        this.svSize = 120;
        this.hueW = 14;
        this.alphaH = hasAlpha ? 12 : 0;

        int blockW = svSize + GAP + hueW;
        int blockX = cx - blockW / 2;
        int blockH = svSize + 10 + alphaH + 12 + CONTROL_HEIGHT;
        int buttonsY = this.height - 30;
        int top = Mth.clamp((this.height - blockH) / 2, HEADER + 8, buttonsY - blockH - 8);

        this.svX = blockX;
        this.svY = top;
        this.hueX = svX + svSize + GAP;
        this.hueY = svY;
        this.hueH = svSize;
        this.alphaX = svX;
        this.alphaY = svY + svSize + 10;
        this.alphaW = blockW;

        this.control = new ColorFieldWidget(blockW, CONTROL_HEIGHT, currentColor(), hasAlpha, this::onControlColorChanged, null);
        this.control.setPosition(blockX, alphaY + alphaH + 12);
        this.addRenderableWidget(this.control);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> {
            onApply.accept(currentColor());
            onClose();
        }).bounds(cx - 100, this.height - 30, 96, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(cx + 4, this.height - 30, 96, 20).build());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private int currentColor() {
        int argb = Mth.hsvToArgb(hue, sat, val, Math.round(alpha * 255));
        return hasAlpha ? argb : argb & 0xFFFFFF;
    }

    private void syncControl() {
        this.suppressControlSync = true;
        this.control.setColor(currentColor());
        this.suppressControlSync = false;
    }

    private void onControlColorChanged(int c) {
        if (suppressControlSync) return;
        float[] hsv = ColorUtils.argbToHsv(c);
        this.hue = hsv[0];
        this.sat = hsv[1];
        this.val = hsv[2];
        if (hasAlpha) this.alpha = FastColor.ARGB32.alpha(c) / 255f;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY, svX, svY, svSize, svSize)) {
            dragging = Drag.SV;
            updateDrag(mouseX, mouseY);
            return true;
        }
        if (isMouseOver(mouseX, mouseY, hueX, hueY, hueW, hueH)) {
            dragging = Drag.HUE;
            updateDrag(mouseX, mouseY);
            return true;
        }
        if (isMouseOver(mouseX, mouseY, alphaX, alphaY, alphaW, alphaH)) {
            dragging = Drag.ALPHA;
            updateDrag(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging != Drag.NONE) {
            updateDrag(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = Drag.NONE;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateDrag(double mouseX, double mouseY) {
        switch (dragging) {
            case SV -> {
                this.sat = Mth.clamp((float) (mouseX - svX) / svSize, 0, 1);
                this.val = Mth.clamp(1 - (float) (mouseY - svY) / svSize, 0, 1);
            }
            case HUE -> this.hue = Mth.clamp((float) (mouseY - hueY) / hueH, 0, 1);
            case ALPHA -> this.alpha = Mth.clamp((float) (mouseX - alphaX) / alphaW, 0, 1);
        }
        syncControl();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, CommonColors.WHITE);

        renderSvSquare(graphics);
        renderHueBar(graphics);
        if (hasAlpha) renderAlphaBar(graphics);
    }

    private void renderSvSquare(GuiGraphics graphics) {
        for (int i = 0; i < svSize; i++) {
            float s = (float) i / svSize;
            int top = Mth.hsvToArgb(hue, s, 1f, 255);
            graphics.fillGradient(svX + i, svY, svX + i + 1, svY + svSize, top, CommonColors.BLACK);
        }
        graphics.renderOutline(svX - 1, svY - 1, svSize + 2, svSize + 2, CommonColors.BLACK);
        int cxp = svX + Math.round(sat * svSize);
        int cyp = svY + Math.round((1 - val) * svSize);
        ring(graphics, cxp, cyp);
    }

    private void renderHueBar(GuiGraphics graphics) {
        for (int i = 0; i < hueH; i++) {
            graphics.fill(hueX, hueY + i, hueX + hueW, hueY + i + 1, Mth.hsvToArgb((float) i / hueH, 1, 1, 255));
        }
        graphics.renderOutline(hueX - 1, hueY - 1, hueW + 2, hueH + 2, CommonColors.BLACK);
        int y = hueY + Math.round(hue * hueH);
        graphics.fill(hueX - 2, y - 1, hueX + hueW + 2, y + 1, CommonColors.WHITE);
    }

    private void renderAlphaBar(GuiGraphics graphics) {
        ColorSwatchWidget.renderChecker(graphics, alphaX, alphaY, alphaW, alphaH);
        int rgb = currentColor() & 0x00FFFFFF;
        for (int i = 0; i < alphaW; i++) {
            int a = Math.round((float) i / alphaW * 255);
            graphics.fill(alphaX + i, alphaY, alphaX + i + 1, alphaY + alphaH, (a << 24) | rgb);
        }
        graphics.renderOutline(alphaX - 1, alphaY - 1, alphaW + 2, alphaH + 2, CommonColors.BLACK);
        int x = alphaX + Math.round(alpha * alphaW);
        graphics.fill(x - 1, alphaY - 2, x + 1, alphaY + alphaH + 2, CommonColors.WHITE);
    }

    private static void ring(GuiGraphics graphics, int cx, int cy) {
        graphics.renderOutline(cx - 3, cy - 3, 6, 6, CommonColors.WHITE);
        graphics.renderOutline(cx - 4, cy - 4, 8, 8, CommonColors.BLACK);
    }
}
