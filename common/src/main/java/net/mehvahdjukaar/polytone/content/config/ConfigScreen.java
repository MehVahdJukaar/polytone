package net.mehvahdjukaar.polytone.content.config;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.StrUtils;
import net.mehvahdjukaar.polytone.common.gui.ChatBubbleWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.minecraft.client.Options.genericValueLabel;

public class ConfigScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("screen.polytone.configs.title");

    private final Multimap<String, OptionHolder<?>> optionsPerCategory =
            MultimapBuilder.linkedHashKeys().arrayListValues().build();
    private final Runnable saveFunc;

    private HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private @Nullable CollapsibleOptionsList list;
    private final Set<String> collapsedNamespaces = new LinkedHashSet<>();
    private final ConfigPresets presets = new ConfigPresets(this::refreshOptionWidgets);

    private @Nullable SpriteIconButton heartButton;
    private @Nullable EditorButton editorButton;
    private @Nullable ChatBubbleWidget supportBubble;
    private @Nullable ChatBubbleWidget editorBubble;
    private boolean editorAvailable;

    public ConfigScreen(Screen lastScreen, Collection<OptionHolder<?>> options, Runnable saveFunc) {
        super(lastScreen, Minecraft.getInstance().options, TITLE);
        for (OptionHolder<?> e : options) {
            this.optionsPerCategory.put(e.fileId.getNamespace(), e);
        }
        this.saveFunc = saveFunc;
    }

    public ConfigScreen(Screen lastScreen, Multimap<String, OptionHolder<?>> options, Runnable saveFunc) {
        super(lastScreen, Minecraft.getInstance().options, TITLE);
        this.optionsPerCategory.putAll(options);
        this.saveFunc = saveFunc;
    }
    @Override
    protected void addOptions() {
    }

    @Override
    public void removed() {
        saveFunc.run();
    }

    @Override
    protected void init() {
        this.layout = new HeaderAndFooterLayout(this);
        this.presets.clear();
        this.list = null;
        this.clearFocus();

        this.list = new CollapsibleOptionsList(this.minecraft, this.width, this);
        populateList(this.list);
        this.layout.addTitleHeader(TITLE, this.font);
        this.layout.addToContents(this.list);

        LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
        int btnWidth = Mth.positiveCeilDiv(150 * 2 - 8, 3);
        boolean inGame = Minecraft.getInstance().level != null;
        boolean packEditor = net.mehvahdjukaar.polytone.PlatStuff.isModLoaded("nautilus_studio");
        Component editorTooltip = !packEditor
                ? Component.translatable("screen.polytone.configs.codec_editor.no_mod")
                : Component.translatable(inGame ? "screen.polytone.configs.codec_editor"
                        : "screen.polytone.configs.codec_editor.disabled");
        EditorButton editorButton = new EditorButton(20, 12, 12, packEditor, editorTooltip);
        editorButton.active = packEditor ? inGame : true;
        this.editorAvailable = packEditor;
        this.editorButton = editorButton;
        footer.addChild(editorButton);

        footer.addChild(Button.builder(Component.translatable("screen.polytone.configs.reset"),
                        b -> resetAndRebuild())
                .width(btnWidth).build());
        footer.addChild(Button.builder(Component.translatable("screen.polytone.configs.undo"),
                        b -> undoAndRebuild())
                .width(btnWidth).build());
        footer.addChild(Button.builder(CommonComponents.GUI_DONE,
                        b -> this.minecraft.setScreen(this.lastScreen))
                .width(btnWidth).build());

        SpriteIconButton heart = SpriteIconButton.builder(
                        Component.translatable("screen.polytone.support.title"),
                        b -> this.minecraft.setScreen(new SupportScreen(this)),
                        true)
                .size(20, 20)
                .sprite(Polytone.res("heart"), 16, 16)
                .build();
        heart.setTooltip(Tooltip.create(Component.translatable("screen.polytone.support.tooltip")));
        this.heartButton = heart;
        footer.addChild(heart);

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.list != null) {
            this.list.updateSize(this.width, this.layout);
        }
    }

    // gap before the impact line, matching the 4px tooltip image margins
    private static final int IMPACT_GAP = 4;

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        renderCustomTooltip(guiGraphics, mouseX, mouseY);
        renderSupportBubble(guiGraphics, mouseX, mouseY, partialTick);
        renderEditorBubble(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderEditorBubble(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.editorAvailable || this.editorButton == null || !this.editorButton.visible) return;

        Component message = Polytone.CONFIGS.bubbleManager.getEditorButtonMessage();
        if (message == null) return;

        if (this.editorBubble == null) {
            this.editorBubble = new ChatBubbleWidget(0, 0, message).setAnimated(true);
        } else if (!message.equals(this.editorBubble.getMessage())) {
            this.editorBubble.setText(message);
        }
        this.editorBubble.renderPointingAt(guiGraphics, this.editorButton, this.width, mouseX, mouseY, partialTick);
    }

    private void renderSupportBubble(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.heartButton == null || !this.heartButton.visible) return;

        Component message = Polytone.CONFIGS.bubbleManager.getHeartButtonMessage();
        if (message == null) return;

        if (this.supportBubble == null) {
            this.supportBubble = new ChatBubbleWidget(0, 0, message).setAnimated(true);
        } else if (!message.equals(this.supportBubble.getMessage())) {
            this.supportBubble.setText(message);
        }
        this.supportBubble.renderPointingAt(guiGraphics, this.heartButton, this.width, mouseX, mouseY, partialTick);
    }

    private void renderCustomTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (this.list == null) return;

        for (OptionHolder<?> holder : optionsPerCategory.values()) {
            if (!(holder.option.values() instanceof PolyConfig<?> config)) continue;
            boolean hasImpact = config.getPerformanceImpact().isPresent();
            if (config.getTooltipImages().isEmpty() && !hasImpact) continue; // plain entries use the built-in tooltip
            AbstractWidget widget = this.list.findOption(holder.option);
            if (widget == null || !widget.isHovered()) continue;

            List<ClientTooltipComponent> components = new ArrayList<>();
            String tooltipKey = holder.fileId.toLanguageKey("config", "tooltip");
            if (I18n.exists(tooltipKey)) {
                Component text = Component.translatable(tooltipKey);
                for (FormattedCharSequence line : this.font.split(text, 170)) {
                    components.add(ClientTooltipComponent.create(line));
                }
            }
            PolyConfig.TooltipImage image = config.getTooltipImages().get(String.valueOf(holder.get()));
            boolean hasImage = image != null;
            if (hasImage) {
                components.add(new ClientImageTooltip(image.texture(), image.width(), image.height()));
            }
            if (hasImpact) {
                if (!hasImage && !components.isEmpty()) {
                    components.add(new SpacerTooltip(IMPACT_GAP));
                }
                Component impactLine = Component.translatable("polytone.tooltip.performance_impact",
                                config.getPerformanceImpact().get().getDisplayName())
                        .withStyle(ChatFormatting.GRAY);
                components.add(ClientTooltipComponent.create(impactLine.getVisualOrderText()));
            }

            if (!components.isEmpty()) {
                guiGraphics.tooltip(this.font, components, mouseX, mouseY,
                        DefaultTooltipPositioner.INSTANCE, null);
            }
            return; // at most one option is hovered at a time
        }
    }

    private record SpacerTooltip(int height) implements ClientTooltipComponent {
        @Override
        public int getWidth(Font font) {
            return 0;
        }

        @Override
        public int getHeight(Font font) {
            return height;
        }
    }

    private void populateList(CollapsibleOptionsList list) {
        for (String modId : optionsPerCategory.keySet()) {
            boolean expanded = !collapsedNamespaces.contains(modId);
            list.addNamespaceHeader(namespaceTitle(modId), expanded, () -> toggleNamespace(modId));
            if (!expanded) continue;

            Collection<OptionHolder<?>> options = optionsPerCategory.get(modId);
            List<OptionHolder<?>> sorted = options.stream()
                    .sorted(Comparator.comparingInt((OptionHolder<?> o) -> {
                                        if (o.option.values() instanceof PolyConfig<?> c) {
                                            return c.getDisplayOrder();
                                        }
                                        return 0;
                                    })
                                    .thenComparing(o -> o.fileId)
                    )
                    .toList();

            Map<Optional<String>, List<OptionHolder<?>>> bySection = new LinkedHashMap<>();
            for (OptionHolder<?> holder : sorted) {
                bySection.computeIfAbsent(sectionOf(holder), k -> new ArrayList<>()).add(holder);
            }

            Map<String, List<ConfigPresets.Action<?>>> overall = new LinkedHashMap<>();
            Map<Optional<String>, Map<String, List<ConfigPresets.Action<?>>>> sectionSliders = new LinkedHashMap<>();
            for (OptionHolder<?> holder : sorted) {
                ConfigPresets.collect(overall, holder.option, false);
                Optional<String> section = sectionOf(holder);
                if (section.isPresent()) {
                    ConfigPresets.collect(
                            sectionSliders.computeIfAbsent(section, k -> new LinkedHashMap<>()),
                            holder.option, true);
                } else {
                    // section_presets on a sectionless entry has no section slider to live on;
                    // fold it into the pack-wide slider rather than dropping it silently.
                    ConfigPresets.collect(overall, holder.option, true);
                }
            }

            addPresetSlider(list, modId, overall, null);

            for (Optional<String> section : orderedSections(bySection)) {
                List<OptionHolder<?>> group = bySection.get(section);
                if (group == null || group.isEmpty()) continue;
                section.ifPresent(s -> {
                    list.addSectionHeader(sectionTitle(modId, s));
                    addPresetSlider(list, modId, sectionSliders.get(section), s);
                });
                addOptionRows(list, group);
            }
        }
    }

    private void toggleNamespace(String modId) {
        if (collapsedNamespaces.contains(modId)) {
            collapsedNamespaces.remove(modId);
        } else {
            collapsedNamespaces.add(modId);
        }
        rebuildPreservingScroll();
    }

    // Decides the top-to-bottom order of sections: the sectionless group first, then named sections by their
    // section_order (smallest declared among the section's entries), with sections that declare none falling
    // back to alphabetical by id.
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

    private void addOptionRows(OptionsList list, List<OptionHolder<?>> group) {
        List<OptionInstance<?>> pending = new ArrayList<>();
        for (OptionHolder<?> holder : group) {
            boolean wide = holder.option.values() instanceof PolyConfig<?> c && c.isWide();
            if (wide) {
                flushSmall(list, pending);
                list.addBig(holder.option);
            } else {
                pending.add(holder.option);
            }
        }
        flushSmall(list, pending);
    }

    private static void flushSmall(OptionsList list, List<OptionInstance<?>> pending) {
        if (pending.isEmpty()) return;
        list.addSmall(pending.toArray(OptionInstance<?>[]::new));
        pending.clear();
    }

    private static Optional<String> sectionOf(OptionHolder<?> holder) {
        return holder.option.values() instanceof PolyConfig<?> c ? c.getSection() : Optional.empty();
    }

    private void addPresetSlider(OptionsList list, String modId,
                                 @Nullable Map<String, List<ConfigPresets.Action<?>>> actions,
                                 @Nullable String section) {
        if (actions == null || actions.isEmpty()) return;
        list.addBig(presets.makeOption(actions, modId, section));
    }

    private void rebuildPreservingScroll() {
        double scroll = this.list != null ? this.list.scrollAmount() : 0;
        this.rebuildWidgets();
        if (this.list != null) {
            this.list.setScrollAmount(scroll);
        }
    }

    private void resetAndRebuild() {
        presets.runSuppressed(() -> {
            for (var option : optionsPerCategory.values()) {
                option.resetToDefault();
            }
        });
        rebuildPreservingScroll();
    }

    // Called by OptionHolder on any option value change so the preset sliders re-derive their position (hand-
    // tweaking an option snaps the slider to Custom, vanilla-style).
    static void onOptionValueChanged() {
        if (Minecraft.getInstance().screen instanceof ConfigScreen cs) {
            cs.presets.rederiveAll();
        }
    }

    private void refreshOptionWidgets(Collection<OptionInstance<?>> options) {
        if (this.list == null) return;
        for (OptionInstance<?> option : options) {
            this.list.resetOption(option);
        }
    }

    private void undoAndRebuild() {
        presets.runSuppressed(() -> {
            for (var entry : optionsPerCategory.asMap().entrySet()) {
                for (var option : entry.getValue()) option.undoChanges();
            }
        });
        rebuildPreservingScroll();
    }

    private Component namespaceTitle(String modId) {
        return Component.translatableWithFallback("config." + modId + ".header",
                StrUtils.readableName(modId));
    }

    private Component sectionTitle(String modId, String section) {
        return Component.translatableWithFallback("config." + modId + ".section." + section,
                StrUtils.readableName(section));
    }


}
