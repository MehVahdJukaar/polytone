package net.mehvahdjukaar.polytone.content.packinfo;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
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
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PackInfoScreen extends Screen {

    private static final ResourceLocation INWORLD_MENU_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_background.png");

    private static final int MAX_TEXT_WIDTH = 280;
    private static final int TITLE_MARGIN = 8;
    private static final int MIN_HEADER_HEIGHT = 33;
    private static final int FOOTER_HEIGHT = 44;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLL_RATE = 12;

    private final Screen lastScreen;
    private final Component heading;
    private final Component body;
    private final List<TextBlock> scrollingBlocks = new ArrayList<>();

    @Nullable
    private TextBlock headerTitle;
    private int headerHeight = MIN_HEADER_HEIGHT;
    private int maxScroll;
    private double scrollAmount;
    private boolean draggingScrollbar;

    public PackInfoScreen(Screen lastScreen, Component packName, PackInfo info) {
        super(info.title().orElse(packName));
        this.lastScreen = lastScreen;
        // yellow and gray are only defaults: a color on the author's own component overrides them
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
        this.scrollingBlocks.clear();
        this.headerTitle = null;

        TextBlock titleBlock = makeTextBlock(this.heading);
        // a title too tall for the header would bleed over the panel, so let it scroll with the body instead
        boolean titleFitsHeader = titleBlock.getHeight() + TITLE_MARGIN * 2 <= this.height / 3;
        if (titleFitsHeader) {
            this.headerHeight = Math.max(MIN_HEADER_HEIGHT, titleBlock.getHeight() + TITLE_MARGIN * 2);
            titleBlock.setPosition((this.width - titleBlock.getWidth()) / 2,
                    (this.headerHeight - titleBlock.getHeight()) / 2);
            this.headerTitle = this.addRenderableWidget(titleBlock);
        } else {
            this.headerHeight = MIN_HEADER_HEIGHT;
            this.scrollingBlocks.add(titleBlock);
        }

        this.scrollingBlocks.add(makeTextBlock(this.body));
        layoutScrollingBlocks();

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 32, 200, 20).build());
    }

    private TextBlock makeTextBlock(Component text) {
        TextBlock block = new TextBlock(text, this.font, this::onComponentClicked);
        block.setCentered(true).setMaxWidth(textWidth());
        return block;
    }

    // stacks the scrolling blocks under the header, centered vertically while they still fit
    private void layoutScrollingBlocks() {
        int contentHeight = TITLE_MARGIN * (this.scrollingBlocks.size() - 1);
        for (TextBlock block : this.scrollingBlocks) contentHeight += block.getHeight();

        int visibleHeight = panelBottom() - this.headerHeight;
        int y = contentHeight + TITLE_MARGIN * 2 <= visibleHeight
                ? this.headerHeight + (visibleHeight - contentHeight) / 2
                : this.headerHeight + TITLE_MARGIN;

        for (TextBlock block : this.scrollingBlocks) {
            block.setPosition((this.width - block.getWidth()) / 2, y);
            y += block.getHeight() + TITLE_MARGIN;
        }
        this.maxScroll = Math.max(0, y - panelBottom());
        setScrollAmount(this.scrollAmount);
    }

    private void setScrollAmount(double amount) {
        this.scrollAmount = Mth.clamp(amount, 0, this.maxScroll);
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

        int top = this.headerHeight;
        int bottom = panelBottom();
        int scrolledMouseY = (int) (mouseY + this.scrollAmount);

        graphics.enableScissor(0, top, this.width, bottom);
        graphics.pose().pushPose();
        graphics.pose().translate(0, (float) -this.scrollAmount, 0);
        for (TextBlock block : this.scrollingBlocks) {
            block.render(graphics, mouseX, scrolledMouseY, partialTick);
        }
        graphics.pose().popPose();
        graphics.disableScissor();

        renderScrollbar(graphics);

        Style style = styleUnderMouse(mouseX, mouseY);
        if (style != null) graphics.renderComponentHoverEffect(this.font, style, mouseX, mouseY);
    }

    private void renderScrollbar(GuiGraphics graphics) {
        if (this.maxScroll <= 0) return;

        int top = this.headerHeight;
        int visibleHeight = panelBottom() - top;
        int thumbHeight = Mth.clamp(visibleHeight * visibleHeight / (visibleHeight + this.maxScroll), 32, visibleHeight);
        int thumbTop = top + (int) (this.scrollAmount * (visibleHeight - thumbHeight) / this.maxScroll);
        int left = scrollbarLeft();

        graphics.fill(left, top, left + SCROLLBAR_WIDTH, top + visibleHeight, 0xFF000000);
        graphics.fill(left, thumbTop, left + SCROLLBAR_WIDTH, thumbTop + thumbHeight, 0xFF808080);
        graphics.fill(left, thumbTop, left + SCROLLBAR_WIDTH - 1, thumbTop + thumbHeight - 1, 0xFFC0C0C0);
    }

    private int scrollbarLeft() {
        return Math.min(this.width - SCROLLBAR_WIDTH - 4, (this.width + textWidth()) / 2 + 8);
    }

    @Nullable
    private Style styleUnderMouse(double mouseX, double mouseY) {
        if (this.headerTitle != null) {
            Style style = this.headerTitle.styleAt(mouseX, mouseY);
            if (style != null) return style;
        }
        if (mouseY < this.headerHeight || mouseY >= panelBottom()) return null;
        for (TextBlock block : this.scrollingBlocks) {
            Style style = block.styleAt(mouseX, mouseY + this.scrollAmount);
            if (style != null) return style;
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.maxScroll > 0) {
            setScrollAmount(this.scrollAmount - scrollY * SCROLL_RATE);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.maxScroll > 0 && mouseX >= scrollbarLeft()
                && mouseX < scrollbarLeft() + SCROLLBAR_WIDTH
                && mouseY >= this.headerHeight && mouseY < panelBottom()) {
            this.draggingScrollbar = true;
            return true;
        }
        Style style = styleUnderMouse(mouseX, mouseY);
        if (button == 0 && style != null && style.getClickEvent() != null) {
            onComponentClicked(style);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingScrollbar) {
            int visibleHeight = panelBottom() - this.headerHeight;
            int thumbHeight = Mth.clamp(visibleHeight * visibleHeight / (visibleHeight + this.maxScroll), 32, visibleHeight);
            setScrollAmount(this.scrollAmount + dragY * this.maxScroll / Math.max(1, visibleHeight - thumbHeight));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    // Text widgets ship inactive, which makes mouseClicked bail before the link hit test. Activating one
    // also makes it click back at every stray click on plain text, so the sound moved to the click
    // handler, where we know a link was hit.
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
            return super.setMaxWidth(maxWidth);
        }

        @Nullable
        Style styleAt(double mouseX, double mouseY) {
            if (!this.isMouseOver(mouseX, mouseY)) return null;
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
