package net.mehvahdjukaar.polytone.content.config;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class SupportScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.polytone.support.title");

    private static final String KOFI_URL = "https://ko-fi.com/mehvahdjukaar";
    private static final String PATREON_URL = "https://www.patreon.com/user?u=53696377";
    private static final String DISCORD_URL = "https://discord.com/invite/qdKRTDf8Cv";
    private static final String WIKI_URL = "https://github.com/MehVahdJukaar/polytone/wiki";

    private static final Identifier MENU_LIST_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/menu_list_background.png");
    private static final Identifier INWORLD_MENU_LIST_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");

    private record Link(Component label, Component description, int color, String url) {}

    private record Section(Component header, List<Link> links) {}

    private static Link link(String key, int color, String url) {
        return new Link(Component.translatable("screen.polytone.support." + key),
                Component.translatable("screen.polytone.support." + key + ".desc"), color, url);
    }

    private static final List<Section> SECTIONS = List.of(
            new Section(Component.translatable("screen.polytone.support.section.donate"), List.of(
                    link("kofi", 0xFFFF5E5B, KOFI_URL),
                    link("patreon", 0xFFF96854, PATREON_URL))),
            new Section(Component.translatable("screen.polytone.support.section.help"), List.of(
                    link("wiki", 0xFF7FE3A0, WIKI_URL),
                    link("discord", 0xFF5865F2, DISCORD_URL))))
            ;

    private static final int DESCRIPTION_COLOR = 0xFF9A9A9A;
    private static final int HEADER_HEIGHT = 18;
    private static final int LINK_HEIGHT = 21;
    private static final int SECTION_GAP = 18;

    // laid out in init(), so render and click detection agree
    private final List<TextRow> headerRows = new ArrayList<>();
    private final List<LinkRow> linkRows = new ArrayList<>();

    private record TextRow(Component text, int y) {}

    private record LinkRow(Link link, int centerX, int y, int left, int right, int bottom) {}

    private final Screen lastScreen;

    public SupportScreen(Screen lastScreen) {
        super(TITLE);
        this.lastScreen = lastScreen;
        Polytone.CONFIGS.bubbleManager.onSupportPageOpened();
    }

    private int panelTop() {
        return 32;
    }

    private int panelBottom() {
        return this.height - 44;
    }

    @Override
    protected void init() {
        this.headerRows.clear();
        this.linkRows.clear();

        int total = SECTION_GAP * (SECTIONS.size() - 1);
        for (Section s : SECTIONS) {
            total += HEADER_HEIGHT + s.links().size() * LINK_HEIGHT;
        }

        int cx = this.width / 2;
        int y = Math.max(panelTop() + 8, (panelTop() + panelBottom() - total) / 2);
        for (Section s : SECTIONS) {
            this.headerRows.add(new TextRow(s.header(), y));
            y += HEADER_HEIGHT;
            for (Link link : s.links()) {
                int w = Math.max(this.font.width(link.label()), this.font.width(link.description()));
                this.linkRows.add(new LinkRow(link, cx, y, cx - w / 2 - 3, cx + w / 2 + 3, y + 19));
                y += LINK_HEIGHT;
            }
            y += SECTION_GAP;
        }

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> this.onClose())
                .bounds(cx - 100, this.height - 32, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        boolean inWorld = this.minecraft.level != null;
        Identifier listBg = inWorld ? INWORLD_MENU_LIST_BACKGROUND : MENU_LIST_BACKGROUND;
        Identifier headerSep = inWorld ? Screen.INWORLD_HEADER_SEPARATOR : Screen.HEADER_SEPARATOR;
        Identifier footerSep = inWorld ? Screen.INWORLD_FOOTER_SEPARATOR : Screen.FOOTER_SEPARATOR;

        int top = panelTop();
        int bottom = panelBottom();
        graphics.blit(RenderPipelines.GUI_TEXTURED, listBg, 0, top, 0.0F, 0.0F, this.width, bottom - top, 32, 32);
        graphics.blit(RenderPipelines.GUI_TEXTURED, headerSep, 0, top - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
        graphics.blit(RenderPipelines.GUI_TEXTURED, footerSep, 0, bottom, 0.0F, 0.0F, this.width, 2, 32, 2);

        graphics.centeredText(this.font, this.rainbowTitle(), this.width / 2, 14, -1);

        for (TextRow row : this.headerRows) {
            graphics.centeredText(this.font, row.text(), this.width / 2, row.y(), -1);
        }
        for (LinkRow row : this.linkRows) {
            Link link = row.link();
            boolean hovered = mouseX >= row.left() && mouseX <= row.right() && mouseY >= row.y() - 1 && mouseY <= row.bottom();
            int color = hovered ? lighten(link.color()) : link.color();
            graphics.centeredText(this.font, link.label(), row.centerX(), row.y(), color);
            graphics.centeredText(this.font, link.description(), row.centerX(), row.y() + 10, DESCRIPTION_COLOR);
            if (hovered) {
                int w = this.font.width(link.label());
                graphics.fill(row.centerX() - w / 2, row.y() + 9, row.centerX() + w / 2, row.y() + 10, color);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (LinkRow row : this.linkRows) {
                if (event.x() >= row.left() && event.x() <= row.right()
                        && event.y() >= row.y() - 1 && event.y() <= row.bottom()) {
                    defaultHandleClickEvent(new ClickEvent.OpenUrl(URI.create(row.link().url())), this.minecraft, this);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private Component rainbowTitle() {
        String text = this.title.getString();
        MutableComponent out = Component.empty();
        long time = Util.getMillis();
        for (int i = 0; i < text.length(); i++) {
            float hue = (((time / 30.0F) + i * 25.0F) % 360.0F) / 360.0F;
            int rgb = Mth.hsvToRgb(hue, 0.7F, 1.0F);
            out.append(Component.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withBold(true)));
        }
        return out;
    }

    private static int lighten(int argb) {
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 40);
        int g = Math.min(255, ((argb >> 8) & 0xFF) + 40);
        int b = Math.min(255, (argb & 0xFF) + 40);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
}
