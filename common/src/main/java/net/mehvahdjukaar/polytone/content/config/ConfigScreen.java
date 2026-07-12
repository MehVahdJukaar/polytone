package net.mehvahdjukaar.polytone.content.config;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.gui.PointingChatBubbleOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("screen.polytone.configs.title");

    private final Multimap<String, OptionHolder<?>> opt = MultimapBuilder.linkedHashKeys().arrayListValues().build();
    /** Namespaces the user has collapsed; their option rows are hidden until re-expanded. */
    private final Set<String> collapsed = new HashSet<>();
    private final Runnable safeFunc;

    @Nullable
    private SpriteIconButton heartButton;
    @Nullable
    private SpriteIconButton editorButton;

    // Shadows OptionsSubScreen.layout (which is final and built once in the constructor). That
    // inherited layout is never emptied, so re-running the vanilla init on every namespace toggle
    // (via rebuildWidgets) keeps re-appending the list/footer/title to it and visitWidgets then
    // re-registers every stale copy — the "widgets pile up on collapse" bug. We instead run our
    // own header/footer flow against a fresh layout each init, leaving the inherited one unused.
    private HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public ConfigScreen(Screen screen, Collection<OptionHolder<?>> options, Runnable safeFunc) {
        super(screen, Minecraft.getInstance().options, TITLE);
        for (OptionHolder<?> e : options) {
            opt.put(e.fileId.getNamespace(), e);
        }
        this.safeFunc = safeFunc;
    }

    @Override
    protected void init() {
        // Fresh layout every (re)init so a namespace toggle doesn't stack duplicate widgets;
        // we deliberately do NOT call super.init(), which would drive the inherited final layout.
        this.layout = new HeaderAndFooterLayout(this);
        this.heartButton = null;
        this.editorButton = null;
        this.clearFocus();

        this.list = new OptionsList(this.minecraft, this.width, this);
        addOptions();
        this.layout.addTitleHeader(TITLE, this.font);
        this.layout.addToContents(this.list);
        addFooter();

        this.layout.visitWidgets(this::addRenderableWidget);
        repositionElements();

        if (this.heartButton != null) {
            this.addRenderableOnly(new PointingChatBubbleOverlay(
                    this.heartButton,
                    () -> this.width,
                    Polytone.CONFIGS.bubbleManager::getHeartButtonMessage));
        }
    }

    @Override
    public void removed() {
        safeFunc.run();
    }

    /** Reset every option to its declared default. */
    private void resetValues() {
        for (OptionHolder<?> holder : opt.values()) {
            holder.resetToDefault();
        }
        rebuildPreservingScroll();
    }

    /** Revert every option to the value currently on disk (the last saved/loaded state). */
    private void undoValues() {
        for (OptionHolder<?> holder : opt.values()) {
            holder.undoChanges();
        }
        rebuildPreservingScroll();
    }

    /**
     * Option buttons are value snapshots, so a programmatic change (reset/undo) needs a rebuild to
     * refresh their labels. Rebuild is now cheap and duplicate-free (fresh layout each init); we
     * only restore the scroll position so the list doesn't jump.
     */
    private void rebuildPreservingScroll() {
        double scroll = this.list != null ? this.list.scrollAmount() : 0;
        this.rebuildWidgets();
        if (this.list != null) {
            this.list.setScrollAmount(scroll);
        }
    }

    @Override
    protected void addFooter() {
        int buttonW = 20;
        // Centered Reset / Undo / Done row. Widths sized so three buttons fit the vanilla footer.
        int textBtnW = Mth.positiveCeilDiv(150 * 2 - 8, 3);
        LinearLayout linearLayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        linearLayout.addChild(Button.builder(Component.translatable("screen.polytone.configs.reset"),
                b -> resetValues()).width(textBtnW).build());
        linearLayout.addChild(Button.builder(Component.translatable("screen.polytone.configs.undo"),
                b -> undoValues()).width(textBtnW).build());
        linearLayout.addChild(Button.builder(CommonComponents.GUI_DONE,
                b -> this.minecraft.setScreen(this.lastScreen)).width(textBtnW).build());

        // Corner icon buttons: free widgets pinned to the screen edges in repositionElements().
        // Nautilus Studio pack-editor button on the LEFT, only when that mod is installed.
        if (PlatStuff.isModLoaded("nautilus_studio")) {
            this.editorButton = this.addRenderableWidget(SpriteIconButton.builder(
                            Component.translatable("screen.polytone.editor.open"),
                            b -> openEditor(),
                            true)
                    .size(buttonW, 20)
                    .sprite(Polytone.res("codec_editor"), 16, 16)
                    .build());
        }
        // Support/heart button on the RIGHT.
        this.heartButton = this.addRenderableWidget(SpriteIconButton.builder(
                        Component.translatable("screen.polytone.support.title"),
                        b -> this.minecraft.setScreen(new SupportScreen(this)),
                        true)
                .size(buttonW, 20)
                .sprite(Polytone.res("heart"), 16, 16)
                .build());
    }

    @Override
    protected void repositionElements() {
        // Arrange our shadow layout (mirrors OptionsSubScreen#repositionElements, but on our layout).
        this.layout.arrangeElements();
        if (this.list != null) {
            this.list.updateSize(this.width, this.layout);
        }
        int margin = 8;
        int footerH = this.layout.getFooterHeight();
        int y = this.height - footerH + (footerH - 20) / 2;
        if (this.editorButton != null) {
            this.editorButton.setPosition(margin, y);
        }
        if (this.heartButton != null) {
            this.heartButton.setPosition(this.width - margin - this.heartButton.getWidth(), y);
        }
    }

    /** Boot the Swing editor off-thread (heavy schema/window build); focus it if already open. */
    private void openEditor() {
        if (net.mehvahdjukaar.polytone.compat.PolytoneEditor.isOpen()) {
            net.mehvahdjukaar.polytone.compat.PolytoneEditor.open();
            return;
        }
        Thread t = new Thread(() -> {
            try {
                net.mehvahdjukaar.polytone.compat.PolytoneEditor.open();
            } catch (Throwable e) {
                Polytone.LOGGER.error("Failed to open Polytone codec editor", e);
            }
        }, "Polytone-Editor-Boot");
        t.setDaemon(true);
        t.start();
    }

    @Override
    protected void addOptions() {
        for (var cat : opt.keySet()) {
            boolean expanded = !collapsed.contains(cat);
            // Clickable namespace header (chevron + bold title); toggling rebuilds the list.
            NamespaceHeaderWidget header = new NamespaceHeaderWidget(
                    this.list.getRowWidth(), 20, getCategoryHeader(cat), expanded, b -> toggleNamespace(cat));
            this.list.addSmall(List.<AbstractWidget>of(header));
            if (expanded) {
                this.list.addSmall(opt.get(cat).stream()
                        .map(h -> h.option).toArray(OptionInstance[]::new));
            }
        }
    }

    private void toggleNamespace(String cat) {
        if (!collapsed.remove(cat)) collapsed.add(cat);
        this.rebuildWidgets();
    }

    private static Component getCategoryHeader(String modId) {
        String key = "config." + modId + ".header";
        if (I18n.exists(key)) return Component.translatable(key);
        return Component.literal(getReadableName(modId));
    }

    private static String getReadableName(String name) {
        return Arrays.stream(name.replace(":", "_").split("_"))
                .map(StringUtils::capitalize).collect(Collectors.joining(" "));
    }
}
