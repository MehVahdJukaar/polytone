package net.mehvahdjukaar.polytone.content.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.List;

// OptionsList with a collapsible namespace header row (chevron + bold title) and a tighter section sub-header.
// Sections are not collapsible; only namespaces are.
public class CollapsibleOptionsList extends OptionsList {
    // Namespace header: the clickable/highlighted strip, plus a gap above (to separate namespaces)
    // and a small gap below (so the first widget under it isn't cramped when no section follows).
    private static final int HEADER_HEIGHT = 11;
    private static final int HEADER_PADDING_TOP = 10;
    private static final int HEADER_PADDING_BOTTOM = 2;
    // Section sub-header: much less top gap than vanilla addHeader (18px), which looked too airy.
    private static final int SECTION_TEXT_HEIGHT = 9;
    private static final int SECTION_PADDING_TOP = 6;
    private static final int SECTION_PADDING_BOTTOM = 2;

    private final OptionsSubScreen ownerScreen;

    public CollapsibleOptionsList(Minecraft minecraft, int width, OptionsSubScreen screen) {
        super(minecraft, width, screen);
        this.ownerScreen = screen;
    }

    public void addNamespaceHeader(Component title, boolean expanded, Runnable onToggle) {
        int paddingTop = this.children().isEmpty() ? 0 : HEADER_PADDING_TOP;
        this.addEntry(new NamespaceHeaderEntry(this.ownerScreen, title, expanded, onToggle, paddingTop),
                paddingTop + HEADER_HEIGHT + HEADER_PADDING_BOTTOM);
    }

    // Tighter alternative to vanilla addHeader(Component) for non-collapsible sub-groups
    public void addSectionHeader(Component title) {
        this.addEntry(new SectionHeaderEntry(this.ownerScreen, title),
                SECTION_PADDING_TOP + SECTION_TEXT_HEIGHT + SECTION_PADDING_BOTTOM);
    }

    private static final class NamespaceHeaderEntry extends AbstractEntry {
        private final OptionsSubScreen screen;
        private final NamespaceHeaderWidget widget;
        private final int paddingTop;

        NamespaceHeaderEntry(OptionsSubScreen screen, Component title, boolean expanded,
                             Runnable onToggle, int paddingTop) {
            this.screen = screen;
            this.paddingTop = paddingTop;
            Button.OnPress press = button -> onToggle.run();
            this.widget = new NamespaceHeaderWidget(310, HEADER_HEIGHT, title, expanded, press);
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                  boolean isHovering, float partialTick) {
            int x = this.screen.width / 2 - 155;
            // Push the widget below the top gap; the bottom gap is just empty row space under it.
            this.widget.setPosition(x, this.getContentY() + this.paddingTop);
            this.widget.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.widget);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.widget);
        }
    }

    private static final class SectionHeaderEntry extends AbstractEntry {
        private final OptionsSubScreen screen;
        private final StringWidget widget;

        SectionHeaderEntry(OptionsSubScreen screen, Component title) {
            this.screen = screen;
            this.widget = new StringWidget(title, screen.getFont());
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                  boolean isHovering, float partialTick) {
            this.widget.setPosition(this.screen.width / 2 - 155, this.getContentY() + SECTION_PADDING_TOP);
            this.widget.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.widget);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.widget);
        }
    }
}
