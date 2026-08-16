package net.mehvahdjukaar.polytone.content.packinfo;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractTextAreaWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

// A pack's info page: a heading and a body, both straight from its pack.mcmeta. Both are MultiLineTextWidgets
// with a component click handler, which is what buys us wrapping, clickable links, hover tooltips and the
// pointer cursor without any layout or hit-testing here.
public class PackInfoScreen extends Screen {

    private static final Identifier INWORLD_MENU_BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/inworld_menu_background.png");

    private static final int MAX_TEXT_WIDTH = 280;
    private static final int TITLE_MARGIN = 8;
    private static final int MIN_HEADER_HEIGHT = 33;
    private static final int FOOTER_HEIGHT = 44;

    private final Screen lastScreen;
    private final Component heading;
    private final Component body;

    private int headerHeight = MIN_HEADER_HEIGHT;

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
        TextBlock titleBlock = makeTextBlock(this.heading);
        this.headerHeight = Math.max(MIN_HEADER_HEIGHT, titleBlock.getHeight() + TITLE_MARGIN * 2);
        titleBlock.setPosition((this.width - titleBlock.getWidth()) / 2,
                (this.headerHeight - titleBlock.getHeight()) / 2);
        this.addRenderableWidget(titleBlock);

        TextBlock bodyBlock = makeTextBlock(this.body);
        // panel only grows to what the text needs, so short content stays centered and doesn't scroll
        int available = panelBottom() - this.headerHeight;
        int panelWidth = textWidth() + 4;
        int panelHeight = Math.min(available, bodyBlock.getHeight() + TITLE_MARGIN * 2);
        this.addRenderableWidget(new BodyPanel((this.width - panelWidth) / 2,
                this.headerHeight + (available - panelHeight) / 2, panelWidth, panelHeight, bodyBlock));

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 32, 200, 20).build());
    }

    private TextBlock makeTextBlock(Component text) {
        TextBlock block = new TextBlock(text, this.font);
        block.setCentered(true).setMaxWidth(textWidth());
        block.setComponentClickHandler(this::onComponentClicked);
        return block;
    }

    private void onComponentClicked(Style style) {
        ClickEvent event = style.getClickEvent();
        if (event == null) return;
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        defaultHandleClickEvent(event, this.minecraft, this);
    }

    private int panelBottom() {
        return this.height - FOOTER_HEIGHT;
    }

    // drawn here rather than in render() so the text widgets end up on top of the panel
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        boolean inWorld = this.minecraft.level != null;
        Identifier listBg = inWorld ? INWORLD_MENU_BACKGROUND : Screen.MENU_BACKGROUND;
        Identifier headerSep = inWorld ? Screen.INWORLD_HEADER_SEPARATOR : Screen.HEADER_SEPARATOR;
        Identifier footerSep = inWorld ? Screen.INWORLD_FOOTER_SEPARATOR : Screen.FOOTER_SEPARATOR;

        int top = this.headerHeight;
        int bottom = panelBottom();
        graphics.blit(RenderPipelines.GUI_TEXTURED, listBg, 0, top, 0.0F, 0.0F, this.width, bottom - top, 32, 32);
        graphics.blit(RenderPipelines.GUI_TEXTURED, headerSep, 0, top - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
        graphics.blit(RenderPipelines.GUI_TEXTURED, footerSep, 0, bottom, 0.0F, 0.0F, this.width, 2, 32, 2);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    // Scrolls the body text. Its widget isn't a screen child, so mouse coords get the scroll offset added
    // before they reach it, which also lands vanilla's link hover effect back under the real cursor.
    private static class BodyPanel extends AbstractTextAreaWidget {

        private final TextBlock content;

        BodyPanel(int x, int y, int width, int height, TextBlock content) {
            super(x, y, width, height, content.getMessage());
            this.content = content;
            content.setPosition(x + (width - content.getWidth()) / 2, y + this.innerPadding());
        }

        // the screen already drew the list panel here; the vanilla text box border would look out of place
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
            this.content.render(graphics, mouseX, (int) (mouseY + this.scrollAmount()), partialTick);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (this.isMouseOver(event.x(), event.y())) {
                MouseButtonEvent scrolled = new MouseButtonEvent(event.x(),
                        event.y() + this.scrollAmount(), event.buttonInfo());
                if (this.content.mouseClicked(scrolled, doubleClick)) return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, this.content.getMessage());
        }
    }

    // Text widgets ship inactive, which makes mouseClicked bail before it ever reaches the component click
    // handler. Activating one also makes it click back at every stray click on plain text, so the sound moves
    // to the handler, where we know a link was actually hit.
    private static class TextBlock extends MultiLineTextWidget {

        TextBlock(Component message, Font font) {
            super(message, font);
            this.active = true;
        }

        @Override
        public void playDownSound(SoundManager soundManager) {
        }
    }
}
