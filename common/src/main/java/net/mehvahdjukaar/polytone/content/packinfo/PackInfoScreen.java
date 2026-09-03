package net.mehvahdjukaar.polytone.content.packinfo;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class PackInfoScreen extends Screen {

    private static final ResourceLocation INWORLD_MENU_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_background.png");

    private static final int MAX_TEXT_WIDTH = 280;
    private static final int TITLE_MARGIN = 8;
    private static final int MIN_HEADER_HEIGHT = 33;
    private static final int FOOTER_HEIGHT = 44;

    private final Screen lastScreen;
    private final Component heading;
    private final Component body;

    private TextBlock titleBlock;
    private BodyPanel bodyPanel;
    private int headerHeight = MIN_HEADER_HEIGHT;

    public PackInfoScreen(Screen lastScreen, Component packName, PackInfo info) {
        super(info.title().orElse(packName));
        this.lastScreen = lastScreen;
        this.heading = withDefaultStyle(ChatFormatting.YELLOW, this.title);
        this.body = withDefaultStyle(ChatFormatting.GRAY, info.content().orElse(CommonComponents.EMPTY));
    }

    private static Component withDefaultStyle(ChatFormatting color, Component text) {
        return Component.empty().withStyle(color).append(text);
    }

    private int textWidth() {
        return Math.min(this.width - 40, MAX_TEXT_WIDTH);
    }

    @Override
    protected void init() {
        this.titleBlock = makeTextBlock(this.heading);
        this.headerHeight = Math.max(MIN_HEADER_HEIGHT, this.titleBlock.getHeight() + TITLE_MARGIN * 2);
        this.titleBlock.setPosition((this.width - this.titleBlock.getWidth()) / 2,
                (this.headerHeight - this.titleBlock.getHeight()) / 2);
        this.addRenderableWidget(this.titleBlock);

        TextBlock bodyBlock = makeTextBlock(this.body);
        int available = panelBottom() - this.headerHeight;
        int panelWidth = textWidth() + 4;
        int panelHeight = Math.min(available, bodyBlock.getHeight() + TITLE_MARGIN * 2);
        this.bodyPanel = this.addRenderableWidget(new BodyPanel((this.width - panelWidth) / 2,
                this.headerHeight + (available - panelHeight) / 2, panelWidth, panelHeight,
                bodyBlock, this::onComponentClicked));

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 32, 200, 20).build());
    }

    private TextBlock makeTextBlock(Component text) {
        TextBlock block = new TextBlock(text, this.font, this::onComponentClicked);
        block.setCentered(true).setMaxWidth(textWidth());
        return block;
    }

    private void onComponentClicked(Style style) {
        ClickEvent event = style.getClickEvent();
        if (event == null) return;
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.handleComponentClicked(style);
    }

    private int panelBottom() {
        return this.height - FOOTER_HEIGHT;
    }

    // drawn here rather than in render() so the text widgets end up on top of the panel
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        boolean inWorld = this.minecraft.level != null;
        ResourceLocation listBg = inWorld ? INWORLD_MENU_BACKGROUND : Screen.MENU_BACKGROUND;
        ResourceLocation headerSep = inWorld ? Screen.INWORLD_HEADER_SEPARATOR : Screen.HEADER_SEPARATOR;
        ResourceLocation footerSep = inWorld ? Screen.INWORLD_FOOTER_SEPARATOR : Screen.FOOTER_SEPARATOR;

        int top = this.headerHeight;
        int bottom = panelBottom();
        graphics.blit(listBg, 0, top, 0.0F, 0.0F, this.width, bottom - top, 32, 32);
        graphics.blit(headerSep, 0, top - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
        graphics.blit(footerSep, 0, bottom, 0.0F, 0.0F, this.width, 2, 32, 2);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        Style style = this.titleBlock.styleAt(mouseX, mouseY);
        if (style == null) style = this.bodyPanel.styleUnderMouse(mouseX, mouseY);
        if (style != null) graphics.renderComponentHoverEffect(this.font, style, mouseX, mouseY);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    private static class BodyPanel extends AbstractScrollWidget {

        private final TextBlock content;
        private final Consumer<Style> onStyleClicked;

        BodyPanel(int x, int y, int width, int height, TextBlock content, Consumer<Style> onStyleClicked) {
            super(x, y, width, height, content.getMessage());
            this.content = content;
            this.onStyleClicked = onStyleClicked;
            content.setPosition(x + (width - content.getWidth()) / 2, y + this.innerPadding());
        }

        @Override
        protected void renderBackground(GuiGraphics graphics) {
        }

        @Override
        protected int getInnerHeight() {
            return this.content.getHeight();
        }

        @Override
        protected double scrollRate() {
            return 9.0;
        }

        @Override
        protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.content.render(graphics, mouseX, mouseY, partialTick);
        }

        @Nullable
        Style styleUnderMouse(double mouseX, double mouseY) {
            if (!this.withinContentAreaPoint(mouseX, mouseY)) return null;
            return this.content.styleAt(mouseX, mouseY + this.scrollAmount());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                Style style = styleUnderMouse(mouseX, mouseY);
                if (style != null && style.getClickEvent() != null) {
                    this.onStyleClicked.accept(style);
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, this.content.getMessage());
        }
    }

    private static class TextBlock extends MultiLineTextWidget {

        private final Font font;
        private final Consumer<Style> onStyleClicked;
        private int wrapWidth = Integer.MAX_VALUE;

        TextBlock(Component message, Font font, Consumer<Style> onStyleClicked) {
            super(message, font);
            this.font = font;
            this.onStyleClicked = onStyleClicked;
            this.active = true;
        }

        @Override
        public MultiLineTextWidget setMaxWidth(int maxWidth) {
            this.wrapWidth = maxWidth;
            MultiLineTextWidget widget = super.setMaxWidth(maxWidth);
            this.width = this.getWidth();
            this.height = this.getHeight();
            return widget;
        }

        @Nullable
        Style styleAt(double mouseX, double mouseY) {
            if (mouseX < this.getX() || mouseX >= this.getX() + this.getWidth()) return null;
            if (mouseY < this.getY() || mouseY >= this.getY() + this.getHeight()) return null;
            List<FormattedCharSequence> lines = this.font.split(this.getMessage(), this.wrapWidth);
            int lineIndex = (int) ((mouseY - this.getY()) / 9);
            if (lineIndex < 0 || lineIndex >= lines.size()) return null;

            FormattedCharSequence line = lines.get(lineIndex);
            int lineLeft = this.getX() + this.getWidth() / 2 - this.font.width(line) / 2;
            int offset = (int) (mouseX - lineLeft);
            if (offset < 0) return null;
            return this.font.getSplitter().componentStyleAtWidth(line, offset);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && this.visible) {
                Style style = styleAt(mouseX, mouseY);
                if (style != null && style.getClickEvent() != null) {
                    this.onStyleClicked.accept(style);
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void playDownSound(SoundManager soundManager) {
        }
    }
}
