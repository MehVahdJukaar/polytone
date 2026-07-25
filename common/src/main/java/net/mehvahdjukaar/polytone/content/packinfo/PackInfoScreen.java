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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A pack's info page: a heading and a body, both straight from its pack.mcmeta. Both are
 * {@link MultiLineTextWidget}s, which is what buys us the wrapping and layout for free; clickable
 * links and hover tooltips are hit-tested against the same split the widget renders with.
 */
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
    private final List<TextBlock> textBlocks = new ArrayList<>();

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
        this.textBlocks.clear();

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
        TextBlock block = new TextBlock(text, this.font, this::onComponentClicked);
        block.setCentered(true).setMaxWidth(textWidth());
        this.textBlocks.add(block);
        return this.addRenderableWidget(block);
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
        for (TextBlock block : this.textBlocks) {
            Style style = block.styleAt(mouseX, mouseY);
            if (style != null) {
                graphics.renderComponentHoverEffect(this.font, style, mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    /**
     * Text widgets ship inactive, which makes {@link #mouseClicked} bail before it ever reaches the
     * link hit test. Activating one also makes it click back at every stray click on plain text, so
     * the sound moves to the click handler, where we know a link was actually hit.
     */
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

        /** Style under the cursor, mirroring the centered layout the widget renders with. */
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
