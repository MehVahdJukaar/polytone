package net.mehvahdjukaar.polytone.content.packinfo;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
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
        TextBlock titleBlock = addTextBlock(this.heading);
        this.headerHeight = Math.max(MIN_HEADER_HEIGHT, titleBlock.getHeight() + TITLE_MARGIN * 2);
        titleBlock.setPosition((this.width - titleBlock.getWidth()) / 2,
                (this.headerHeight - titleBlock.getHeight()) / 2);

        TextBlock bodyBlock = addTextBlock(this.body);
        bodyBlock.setPosition((this.width - bodyBlock.getWidth()) / 2,
                Math.max(this.headerHeight + TITLE_MARGIN,
                        (this.headerHeight + panelBottom() - bodyBlock.getHeight()) / 2));

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 32, 200, 20).build());
    }

    private TextBlock addTextBlock(Component text) {
        TextBlock block = new TextBlock(text, this.font);
        block.setCentered(true).setMaxWidth(textWidth());
        block.setComponentClickHandler(this::onComponentClicked);
        return this.addRenderableWidget(block);
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

    // drawn here rather than in extractRenderState() so the text widgets end up on top of the panel
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

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
        this.minecraft.gui.setScreen(this.lastScreen);
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
