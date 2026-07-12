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
import net.minecraft.client.gui.components.SpriteIconButton;
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

    private final Multimap<String, OptionInstance<?>> opt = MultimapBuilder.hashKeys().arrayListValues().build();
    /** Namespaces the user has collapsed; their option rows are hidden until re-expanded. */
    private final Set<String> collapsed = new HashSet<>();
    private final Runnable safeFunc;

    @Nullable
    private SpriteIconButton heartButton;
    @Nullable
    private SpriteIconButton editorButton;

    public ConfigScreen(Screen screen, Collection<OptionHolder<?>> options, Runnable safeFunc) {
        super(screen, Minecraft.getInstance().options, TITLE);
        for (OptionHolder<?> e : options) {
            opt.put(e.fileId.getNamespace(), e.option);
        }
        this.safeFunc = safeFunc;
    }

    @Override
    protected void init() {
        super.init();
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

    private void resetValues() {
        for (var entry : opt.asMap().entrySet()) {
            for (var option : entry.getValue()) {
                resetOptionValue(option);
            }
        }
    }

    private <T> void resetOptionValue(OptionInstance<T> option) {
        if (option.values() instanceof PolyConfig<T> c) {
            option.set(c.getDefaultValue());
        }
    }

    @Override
    protected void addFooter() {
        int buttonW = 20;
        // Centered Reset/Done row, as vanilla.
        LinearLayout linearLayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        linearLayout.addChild(Button.builder(Component.translatable("screen.polytone.configs.reset"),
                b -> resetValues()).build());
        linearLayout.addChild(Button.builder(CommonComponents.GUI_DONE,
                b -> this.minecraft.setScreen(this.lastScreen)).build());

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
        super.repositionElements();
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
                this.list.addSmall(opt.get(cat).toArray(new OptionInstance[0]));
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
