package net.mehvahdjukaar.polytone.content.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class CollapsibleOptionsList extends OptionsList {
    private static final int HEADER_HEIGHT = 15;
    private static final int HEADER_PADDING_TOP = 12;

    private final OptionsSubScreen ownerScreen;

    public CollapsibleOptionsList(Minecraft minecraft, int width, OptionsSubScreen screen) {
        super(minecraft, width, screen);
        this.ownerScreen = screen;
    }

    public void addNamespaceHeader(Component title, boolean expanded, Runnable onToggle) {
        int paddingTop = this.children().isEmpty() ? 0 : HEADER_PADDING_TOP;
        this.addEntry(new NamespaceHeaderEntry(this.ownerScreen, title, expanded, onToggle, paddingTop),
                paddingTop + HEADER_HEIGHT);
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
}
