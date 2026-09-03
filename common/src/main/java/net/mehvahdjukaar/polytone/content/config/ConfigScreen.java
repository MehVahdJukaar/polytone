package net.mehvahdjukaar.polytone.content.config;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.nautilus.PolytoneNautilus;
import net.mehvahdjukaar.polytone.content.common.gui.PointingChatBubbleOverlay;
import net.mehvahdjukaar.polytone.utils.StrUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConfigScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("screen.polytone.configs.title");
    private static final String NAUTILUS_URL = "https://github.com/MehVahdJukaar/pack_editor";

    private final Multimap<String, OptionHolder<?>> opt = MultimapBuilder.linkedHashKeys().arrayListValues().build();
    private final Set<String> collapsed = new HashSet<>();
    private final Runnable safeFunc;

    @Nullable
    private SpriteIconButton heartButton;
    @Nullable
    private EditorIconButton editorButton;
    private boolean editorAvailable;
    private boolean rebuildScheduled;
    private volatile boolean editorBooting;

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

        addBubble(this.heartButton, Polytone.CONFIGS.bubbleManager::getHeartButtonMessage);
        if (!this.editorAvailable) {
            addBubble(this.editorButton, Polytone.CONFIGS.bubbleManager::getEditorButtonMessage);
        }
    }

    private void addBubble(@Nullable AbstractWidget anchor, Supplier<Component> message) {
        if (anchor == null) return;
        this.addRenderableOnly(new PointingChatBubbleOverlay(anchor, () -> this.width, message));
    }

    @Override
    public void removed() {
        safeFunc.run();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void addFooter() {
        int iconW = 20;
        int textBtnW = Mth.positiveCeilDiv(150 * 2 - 8, 3);
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));

        this.editorAvailable = PlatStuff.isModLoaded("nautilus_studio");
        EditorIconButton editor = new EditorIconButton(iconW, 20,
                Component.translatable("screen.polytone.editor.open"),
                editorAvailable, () -> this.editorBooting,
                b -> onEditorPressed());
        editor.setTooltip(Tooltip.create(Component.translatable(
                editorAvailable ? "screen.polytone.editor.open" : "screen.polytone.editor.no_mod")));
        this.editorButton = footer.addChild(editor);
        footer.addChild(Button.builder(Component.translatable("screen.polytone.configs.reset"),
                b -> applyToAll(OptionHolder::resetToDefault)).width(textBtnW).build());
        footer.addChild(Button.builder(Component.translatable("screen.polytone.configs.undo"),
                b -> applyToAll(OptionHolder::undoChanges)).width(textBtnW).build());
        footer.addChild(Button.builder(CommonComponents.GUI_DONE,
                b -> this.minecraft.setScreen(this.lastScreen)).width(textBtnW).build());
        this.heartButton = footer.addChild(SpriteIconButton.builder(
                        Component.translatable("screen.polytone.support.title"),
                        b -> this.minecraft.setScreen(new SupportScreen(this)),
                        true)
                .size(iconW, 20)
                .sprite(Polytone.res("heart"), 16, 16)
                .build());
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.list != null) {
            this.list.updateSize(this.width, this.layout);
        }
    }

    private void onEditorPressed() {
        if (editorAvailable) {
            openEditor();
            return;
        }
        Polytone.CONFIGS.bubbleManager.onEditorButtonClicked();
        this.minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) Util.getPlatform().openUri(NAUTILUS_URL);
            this.minecraft.setScreen(this);
        }, NAUTILUS_URL, true));
    }

    private void openEditor() {
        if (PolytoneNautilus.isOpen()) {
            PolytoneNautilus.open();
            return;
        }
        this.editorBooting = true;
        Thread t = new Thread(() -> {
            try {
                PolytoneNautilus.open();
            } catch (Throwable e) {
                Polytone.LOGGER.error("Failed to open Polytone codec editor", e);
            } finally {
                this.editorBooting = false;
            }
        }, "Polytone-Editor-Boot");
        t.setDaemon(true);
        t.start();
    }

    private void applyToAll(Consumer<OptionHolder<?>> action) {
        opt.values().forEach(action);
        rebuildPreservingScroll();
    }

    private void rebuildPreservingScroll() {
        double scroll = this.list != null ? this.list.getScrollAmount() : 0;
        this.rebuildWidgets();
        if (this.list != null) {
            this.list.setScrollAmount(scroll);
        }
    }

    private void scheduleRebuild() {
        if (rebuildScheduled) return;
        rebuildScheduled = true;
        this.minecraft.execute(() -> {
            rebuildScheduled = false;
            if (this.minecraft.screen == this) rebuildPreservingScroll();
        });
    }

    @Override
    protected void addOptions() {
        for (String modId : opt.keySet()) {
            boolean expanded = !collapsed.contains(modId);
            NamespaceHeaderWidget header = new NamespaceHeaderWidget(this.list.getRowWidth(), 20,
                    namespaceTitle(modId), expanded, b -> toggleNamespace(modId));
            this.list.addSmall(List.of(header));
            if (expanded) addNamespace(modId);
        }
    }

    private void addNamespace(String modId) {
        List<OptionHolder<?>> sorted = opt.get(modId).stream()
                .sorted(Comparator.comparingInt(ConfigScreen::displayOrderOf)
                        .thenComparing(o -> o.fileId))
                .toList();

        Map<Optional<String>, List<OptionHolder<?>>> bySection = new LinkedHashMap<>();
        for (OptionHolder<?> holder : sorted) {
            bySection.computeIfAbsent(sectionOf(holder), k -> new ArrayList<>()).add(holder);
        }

        ConfigPresets packWidePresets = new ConfigPresets(modId, null);
        Map<Optional<String>, ConfigPresets> sectionPresets = new LinkedHashMap<>();
        for (OptionHolder<?> holder : sorted) {
            packWidePresets.collect(holder.option, false);
            Optional<String> section = sectionOf(holder);
            ConfigPresets scoped = section.isEmpty() ? packWidePresets : sectionPresets
                    .computeIfAbsent(section, k -> new ConfigPresets(modId, section.get()));
            scoped.collect(holder.option, true);
        }

        addPresetSlider(packWidePresets);
        for (Optional<String> section : orderedSections(bySection)) {
            section.ifPresent(s -> {
                addSectionHeader(sectionTitle(modId, s));
                addPresetSlider(sectionPresets.get(section));
            });
            addOptionRows(bySection.get(section));
        }
    }

    private void addPresetSlider(@Nullable ConfigPresets presets) {
        if (presets == null || presets.isEmpty()) return;
        this.list.addBig(presets.makeOption(this::scheduleRebuild));
    }

    private void addSectionHeader(Component title) {
        StringWidget widget = new StringWidget(this.list.getRowWidth(), 20,
                title.copy().withStyle(ChatFormatting.GRAY), this.font);
        widget.alignLeft();
        this.list.addSmall(List.of(widget));
    }

    private void addOptionRows(List<OptionHolder<?>> group) {
        List<OptionInstance<?>> pending = new ArrayList<>();
        for (OptionHolder<?> holder : group) {
            if (holder.option.values() instanceof PolyConfig<?> c && c.isWide()) {
                flushSmall(pending);
                this.list.addBig(holder.option);
            } else {
                pending.add(holder.option);
            }
        }
        flushSmall(pending);
    }

    private void flushSmall(List<OptionInstance<?>> pending) {
        if (pending.isEmpty()) return;
        this.list.addSmall(pending.toArray(OptionInstance<?>[]::new));
        pending.clear();
    }

    private void toggleNamespace(String cat) {
        if (!collapsed.remove(cat)) collapsed.add(cat);
        rebuildPreservingScroll();
    }

    private static int displayOrderOf(OptionHolder<?> holder) {
        return holder.option.values() instanceof PolyConfig<?> c ? c.getDisplayOrder() : 0;
    }

    private static Optional<String> sectionOf(OptionHolder<?> holder) {
        return holder.option.values() instanceof PolyConfig<?> c ? c.getSection() : Optional.empty();
    }

    private static List<Optional<String>> orderedSections(Map<Optional<String>, List<OptionHolder<?>>> bySection) {
        return bySection.keySet().stream()
                .sorted(Comparator
                        .comparing((Optional<String> s) -> s.isPresent())          // empty group first
                        .thenComparingInt(s -> sectionSortKey(bySection.get(s)))    // explicit section_order
                        .thenComparing(s -> s.orElse("")))                          // alphabetical fallback
                .toList();
    }

    private static int sectionSortKey(List<OptionHolder<?>> entries) {
        int min = Integer.MAX_VALUE;
        for (OptionHolder<?> holder : entries) {
            if (holder.option.values() instanceof PolyConfig<?> c && c.getSectionOrder().isPresent()) {
                min = Math.min(min, c.getSectionOrder().get());
            }
        }
        return min;
    }

    private static Component namespaceTitle(String modId) {
        return Component.translatableWithFallback("config." + modId + ".header",
                StrUtils.readableName(modId));
    }

    private static Component sectionTitle(String modId, String section) {
        return Component.translatableWithFallback("config." + modId + ".section." + section,
                StrUtils.readableName(section));
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.list == null) return;
        for (OptionHolder<?> holder : opt.values()) {
            if (!(holder.option.values() instanceof PolyConfig<?> config)) continue;
            if (config.getTooltipImages().isEmpty() && config.getPerformanceImpact().isEmpty()) continue;
            AbstractWidget widget = this.list.findOption(holder.option);
            if (widget == null || !widget.isHovered()) continue;

            List<ClientTooltipComponent> lines = richTooltipFor(holder, config);
            if (!lines.isEmpty()) {
                guiGraphics.renderTooltipInternal(this.font, lines, mouseX, mouseY,
                        DefaultTooltipPositioner.INSTANCE);
            }
            return;
        }
    }

    private List<ClientTooltipComponent> richTooltipFor(OptionHolder<?> holder, PolyConfig<?> config) {
        List<ClientTooltipComponent> lines = new ArrayList<>();
        String tooltipKey = holder.fileId.toLanguageKey("config", "tooltip");
        if (I18n.exists(tooltipKey)) {
            for (FormattedCharSequence line : this.font.split(Component.translatable(tooltipKey), 170)) {
                lines.add(ClientTooltipComponent.create(line));
            }
        }
        PolyConfig.TooltipImage image = config.getTooltipImages().get(String.valueOf(holder.get()));
        if (image != null) {
            lines.add(new ClientImageTooltip(image.texture(), image.width(), image.height()));
        }
        config.getPerformanceImpact().ifPresent(impact -> {
            if (image == null && !lines.isEmpty()) lines.add(new SpacerTooltip(4));
            lines.add(ClientTooltipComponent.create(Component
                    .translatable("polytone.tooltip.performance_impact", impact.getDisplayName())
                    .withStyle(ChatFormatting.GRAY)
                    .getVisualOrderText()));
        });
        return lines;
    }

    private record SpacerTooltip(int height) implements ClientTooltipComponent {
        @Override
        public int getWidth(Font font) {
            return 0;
        }

        @Override
        public int getHeight() {
            return height;
        }
    }
}
