package net.mehvahdjukaar.polytone.content.config;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.StrUtils;
import net.mehvahdjukaar.polytone.common.gui.ChatBubbleWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.ResettableOptionWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

import static net.minecraft.client.Options.genericValueLabel;

public class ConfigScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("screen.polytone.configs.title");

    private final Multimap<String, OptionHolder<?>> optionsPerCategory =
            MultimapBuilder.linkedHashKeys().arrayListValues().build();
    private Runnable saveFunc;

    // Shadows OptionsSubScreen.layout (which is final): this screen runs its own header/footer
    // flow instead of the OptionsSubScreen init, so the inherited layout stays unused.
    private HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private @Nullable CollapsibleOptionsList list;
    private final Set<String> collapsedNamespaces = new LinkedHashSet<>();
    private final Map<String, Runnable> presetRederivers = new LinkedHashMap<>();
    private boolean suppressRederive;

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

    /** Required by OptionsSubScreen; the single list is built in {@link #init()}, so this stays empty. */
    @Override
    protected void addOptions() {
    }

    @Override
    public void removed() {
        saveFunc.run();
    }

    @Override
    protected void init() {
        // Fresh layout every (re)init so window resizes don't double-add widgets.
        this.layout = new HeaderAndFooterLayout(this);
        this.presetRederivers.clear();
        this.list = null;
        // Drop stale focus across the rebuild: a widget keeping focus through a window resize
        // can pin its tooltip on screen next to the hovered one (focused tooltips ignore hover).
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

        // Support/heart button (right of the row).
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

    // Nudges people toward the editor, but only when they don't already have it installed.
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

    /**
     * Tooltip for a hovered option with a preview image and/or impact line, drawn like vanilla item
     * tooltips. The built-in text tooltip is suppressed in {@link OptionHolder} for these entries.
     */
    private void renderCustomTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (this.list == null) return;

        for (OptionHolder<?> holder : optionsPerCategory.values()) {
            if (!(holder.option.values() instanceof PolyConfig<?> config)) continue;
            boolean hasImpact = config.getPerformanceImpact().isPresent();
            if (config.getTooltipImages().isEmpty() && !hasImpact) continue; // plain entries use the built-in tooltip
            AbstractWidget widget = this.list.findOption(holder.option);
            if (widget == null || !widget.isHovered()) continue;

            List<ClientTooltipComponent> components = new ArrayList<>();
            // description on top, when the entry has a tooltip translation
            String tooltipKey = holder.fileId.toLanguageKey("config", "tooltip");
            if (I18n.exists(tooltipKey)) {
                Component text = Component.translatable(tooltipKey);
                for (FormattedCharSequence line : this.font.split(text, 170)) {
                    components.add(ClientTooltipComponent.create(line));
                }
            }
            // image in the middle, chosen by the option's current value; its margins supply the gaps
            PolyConfig.TooltipImage image = config.getTooltipImages().get(String.valueOf(holder.get()));
            boolean hasImage = image != null;
            if (hasImage) {
                components.add(new ClientImageTooltip(image.texture(), image.width(), image.height()));
            }
            // Performance impact (bottom). When no image sits above it, add a small spacer so it
            // isn't cramped against the description; an image already provides the gap via its margin.
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
                guiGraphics.renderTooltip(this.font, components, mouseX, mouseY,
                        DefaultTooltipPositioner.INSTANCE, null);
            }
            return; // at most one option is hovered at a time
        }
    }

    /** An empty tooltip line that just reserves vertical space (renders nothing). */
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

    // --- list population ---

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

            // Group entries by section, keeping display_order within each; section placement is
            // decided separately below.
            Map<Optional<String>, List<OptionHolder<?>>> bySection = new LinkedHashMap<>();
            for (OptionHolder<?> holder : sorted) {
                bySection.computeIfAbsent(sectionOf(holder), k -> new ArrayList<>()).add(holder);
            }

            // "presets" feeds the pack-wide slider at the top of the namespace, "section_presets"
            // the slider of the entry's own section. Applying a stop on any slider re-derives the rest.
            Map<String, List<PresetAction<?>>> overall = new LinkedHashMap<>();
            Map<Optional<String>, Map<String, List<PresetAction<?>>>> sectionSliders = new LinkedHashMap<>();
            for (OptionHolder<?> holder : sorted) {
                collectPresetActions(overall, holder.option, false);
                Optional<String> section = sectionOf(holder);
                if (section.isPresent()) {
                    collectPresetActions(
                            sectionSliders.computeIfAbsent(section, k -> new LinkedHashMap<>()),
                            holder.option, true);
                } else {
                    // section_presets on a sectionless entry has no section slider to live on;
                    // fold it into the pack-wide slider rather than dropping it silently.
                    collectPresetActions(overall, holder.option, true);
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

    /**
     * Decides the top-to-bottom order of sections: the sectionless group first, then named
     * sections by their {@code section_order} (smallest declared among the section's entries),
     * with sections that declare none falling back to alphabetical by id.
     */
    private static List<Optional<String>> orderedSections(Map<Optional<String>, List<OptionHolder<?>>> bySection) {
        return bySection.keySet().stream()
                .sorted(Comparator
                        .comparing((Optional<String> s) -> s.isPresent())          // empty group first
                        .thenComparingInt(s -> sectionSortKey(bySection.get(s)))    // explicit section_order
                        .thenComparing(s -> s.orElse("")))                          // alphabetical fallback
                .toList();
    }

    /** Smallest section_order declared by any entry in the section; MAX_VALUE if none declare it. */
    private static int sectionSortKey(List<OptionHolder<?>> entries) {
        int min = Integer.MAX_VALUE;
        for (OptionHolder<?> holder : entries) {
            if (holder.option.values() instanceof PolyConfig<?> c && c.getSectionOrder().isPresent()) {
                min = Math.min(min, c.getSectionOrder().get());
            }
        }
        return min;
    }

    /**
     * Adds a section's options preserving order: entries flagged {@code "wide": true} get their
     * own full-width row (addBig), the rest are packed two-per-row (addSmall). A run of normal
     * options is flushed whenever a wide one interrupts it, so a wide button cleanly breaks up a
     * long list and dodges the lonely half-button an odd count would otherwise leave.
     */
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
                                 @Nullable Map<String, List<PresetAction<?>>> presets,
                                 @Nullable String section) {
        if (presets == null || presets.isEmpty()) return;
        list.addBig(makePresetOpt(presets, modId, section));
    }

    /**
     * Rebuilds every widget from current option values, then restores scroll. Option buttons are
     * value snapshots, so programmatic changes (reset/undo/presets) need this.
     */
    private void rebuildPreservingScroll() {
        double scroll = this.list != null ? this.list.scrollAmount() : 0;
        this.rebuildWidgets();
        if (this.list != null) {
            this.list.setScrollAmount(scroll);
        }
    }

    private void resetAndRebuild() {
        suppressRederive = true;
        try {
            for (var option : optionsPerCategory.values()) {
                option.resetToDefault();
            }
        } finally {
            suppressRederive = false;
        }
        rebuildPreservingScroll();
    }

    /**
     * Called by {@link OptionHolder} on any option value change so the preset sliders re-derive
     * their position (hand-tweaking an option snaps the slider to Custom, vanilla-style).
     */
    static void onOptionValueChanged() {
        if (Minecraft.getInstance().screen instanceof ConfigScreen cs && !cs.suppressRederive) {
            cs.presetRederivers.values().forEach(Runnable::run);
        }
    }

    /**
     * Refreshes the given options' widgets in place via {@code OptionsList#resetOption}, the
     * vanilla live-update path, so labels follow programmatic changes without a screen rebuild.
     */
    private void refreshOptionWidgets(Collection<OptionInstance<?>> options) {
        if (this.list == null) return;
        for (OptionInstance<?> option : options) {
            this.list.resetOption(option);
        }
    }

    private void undoAndRebuild() {
        suppressRederive = true;
        try {
            for (var entry : optionsPerCategory.asMap().entrySet()) {
                for (var option : entry.getValue()) option.undoChanges();
            }
        } finally {
            suppressRederive = false;
        }
        rebuildPreservingScroll();
    }

    // --- header title resolution ---

    private Component namespaceTitle(String modId) {
        return Component.translatableWithFallback("config." + modId + ".header",
                StrUtils.readableName(modId));
    }

    private Component sectionTitle(String modId, String section) {
        return Component.translatableWithFallback("config." + modId + ".section." + section,
                StrUtils.readableName(section));
    }

    // --- preset slider ---
    // Vanilla 25w41a graphics-Preset style: stops live-preview while dragging, snap on release,
    // last stop is Custom (restores the drag-start snapshot). Position is derived from values.

    private OptionInstance<?> makePresetOpt(Map<String, List<PresetAction<?>>> presets, String modId,
                                            @Nullable String section) {
        String rederiveKey = modId + "/" + (section == null ? "" : section);
        List<String> names = List.copyOf(presets.keySet());

        // Every option any preset touches; options outside this set are never previewed/restored.
        Set<OptionInstance<?>> affected = new LinkedHashSet<>();
        for (var actions : presets.values()) {
            for (var action : actions) affected.add(action.option());
        }

        // Current slider position: first fully-matching preset, else the last stop = Custom.
        IntSupplier derive = () -> {
            for (int i = 0; i < names.size(); i++) {
                if (presets.get(names.get(i)).stream().allMatch(PresetAction::matches)) {
                    return i;
                }
            }
            return names.size();
        };
        int current = derive.getAsInt();

        Identifier id = Identifier.fromNamespaceAndPath(modId, "presets");
        String titleKey = id.toLanguageKey();
        // Per-section title override -> pack-wide title -> generic "Preset".
        Component caption = firstTranslated(
                section == null ? null : titleKey + ".section." + section,
                titleKey);

        // General slider tooltip, used as the fallback for stops without their own.
        Component fallbackTooltip = null;
        if (section != null) fallbackTooltip = translatedOrNull(titleKey + ".section." + section + ".tooltip");
        if (fallbackTooltip == null) fallbackTooltip = translatedOrNull(titleKey + ".tooltip");
        // per-stop tooltips, precomputed so the supplier is allocation-free
        Tooltip[] stopTooltips = new Tooltip[names.size() + 1];
        for (int i = 0; i < stopTooltips.length; i++) {
            Component t = presetTooltip(modId, section, i, names, fallbackTooltip);
            stopTooltips[i] = t == null ? null : Tooltip.create(t);
        }
        OptionInstance.TooltipSupplier<Integer> tooltipSupplier =
                index -> (index >= 0 && index < stopTooltips.length) ? stopTooltips[index] : null;

        // snapshot taken at drag start; the Custom stop restores it
        List<Runnable> customSnapshot = new ArrayList<>();
        Runnable onDragStart = () -> {
            customSnapshot.clear();
            for (OptionInstance<?> option : affected) {
                customSnapshot.add(captureRestore(option));
            }
        };
        // Applies a stop's values and live-refreshes the affected widgets. suppressRederive stops
        // the option sets from echoing back through onOptionValueChanged mid-apply.
        IntConsumer applyStop = index -> {
            suppressRederive = true;
            try {
                if (index >= names.size()) {
                    customSnapshot.forEach(Runnable::run);
                } else {
                    presets.get(names.get(index)).forEach(PresetAction::apply);
                }
            } finally {
                suppressRederive = false;
            }
            refreshOptionWidgets(affected);
            // Sliders are layered (overall + per-section): applying a stop here may change
            // what the other sliders should display, so they re-derive.
            rederiveOthersExcept(rederiveKey);
        };

        var opt = new OptionInstance<>(titleKey,
                tooltipSupplier,
                (component, index) -> genericValueLabel(caption, presetValueLabel(modId, section, index, names)),
                new PresetSliderValueSet(names.size(),
                        index -> genericValueLabel(caption, presetValueLabel(modId, section, index, names)),
                        onDragStart, applyStop),
                current, (index) -> {
            // Covers commits that didn't go through drag-preview (keyboard arrows, plain
            // clicks). Custom commits are no-ops here: restoring would replay a stale snapshot.
            if (index < names.size()) {
                applyStop.accept(index);
            }
        });

        // Hand-tweaking any option re-derives the slider position in place (Custom when no
        // preset matches), through the same ResettableOptionWidget refresh as everything else.
        presetRederivers.put(rederiveKey, () -> {
            int index = derive.getAsInt();
            if (!Objects.equals(opt.get(), index)) {
                suppressRederive = true;
                try {
                    opt.set(index);
                } finally {
                    suppressRederive = false;
                }
            }
            refreshOptionWidgets(List.of(opt));
        });
        return opt;
    }

    private static <T> Runnable captureRestore(OptionInstance<T> option) {
        T value = option.get();
        return () -> option.set(value);
    }

    private void rederiveOthersExcept(String exceptKey) {
        presetRederivers.forEach((key, rederiver) -> {
            if (!key.equals(exceptKey)) rederiver.run();
        });
    }

    /** First key with an actual translation wins; generic "Preset" as the final fallback. */
    private static Component firstTranslated(@Nullable String... keys) {
        for (String key : keys) {
            if (key == null) continue;
            Component c = translatedOrNull(key);
            if (c != null) return c;
        }
        return Component.translatable("polytone.preset");
    }

    private static Component presetValueLabel(String modId, @Nullable String section,
                                              int index, List<String> names) {
        if (index >= names.size()) {
            // "Custom" is mod-provided but packs can override it, per slider or pack-wide.
            if (section != null) {
                Component c = translatedOrNull(modId + ".presets.section." + section + ".custom");
                if (c != null) return c;
            }
            Component c = translatedOrNull(modId + ".presets.custom");
            return c != null ? c : Component.translatable("polytone.preset.custom");
        }
        String name = names.get(index);
        // Per-section override first (the same preset name on different section sliders can
        // display differently), then the pack-wide name, then a prettified fallback.
        if (section != null) {
            Component c = translatedOrNull(modId + ".presets.section." + section + "." + name);
            if (c != null) return c;
        }
        return Component.translatableWithFallback(modId + ".presets." + name,
                StrUtils.readableName(name));
    }

    @Nullable
    private static Component translatedOrNull(String key) {
        return I18n.exists(key) ? Component.translatable(key) : null;
    }

    /** Per-stop tooltip: per-section override -> per-preset -> the general slider tooltip. */
    @Nullable
    private static Component presetTooltip(String modId, @Nullable String section, int index,
                                           List<String> names, @Nullable Component fallback) {
        String suffix = (index >= names.size() ? "custom" : names.get(index)) + ".tooltip";
        if (section != null) {
            Component c = translatedOrNull(modId + ".presets.section." + section + "." + suffix);
            if (c != null) return c;
        }
        Component c = translatedOrNull(modId + ".presets." + suffix);
        return c != null ? c : fallback;
    }

    private static <T> void collectPresetActions(Map<String, List<PresetAction<?>>> presets,
                                                 OptionInstance<T> option, boolean sectionScoped) {
        if (option.values() instanceof PolyConfig<T> c) {
            Map<String, T> declared = sectionScoped ? c.getSectionPresets() : c.getPresets();
            for (var entry : declared.entrySet()) {
                presets.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .add(new PresetAction<>(option, entry.getValue()));
            }
        }
    }

    private record PresetAction<T>(OptionInstance<T> option, T value) {
        void apply() {
            option.set(value);
        }

        boolean matches() {
            return option.get().equals(value);
        }
    }

    // ValueSet whose widget is the smooth-drag slider below, mirroring vanilla 25w41a+'s
    // graphics Preset slider feel: free thumb while dragging, live preview, snap on release.
    private record PresetSliderValueSet(int maxIndex, IntFunction<Component> labelGetter,
                                        Runnable onDragStart, IntConsumer onStopPreview)
            implements OptionInstance.ValueSet<Integer> {

        @Override
        public Function<OptionInstance<Integer>, AbstractWidget> createButton(
                OptionInstance.TooltipSupplier<Integer> tooltipSupplier, Options options,
                int x, int y, int width, Consumer<Integer> onChanged) {
            return optionInstance -> new PresetSliderButton(x, y, width, 20,
                    optionInstance, maxIndex, labelGetter, onDragStart, onStopPreview,
                    tooltipSupplier, onChanged);
        }

        @Override
        public Optional<Integer> validateValue(Integer value) {
            return (value >= 0 && value <= maxIndex) ? Optional.of(value) : Optional.empty();
        }

        @Override
        public Codec<Integer> codec() {
            return Codec.intRange(0, maxIndex);
        }
    }

    private static final class PresetSliderButton extends AbstractSliderButton implements ResettableOptionWidget {
        private final OptionInstance<Integer> option;
        private final int maxIndex;
        private final IntFunction<Component> labelGetter;
        private final Runnable onDragStart;
        private final IntConsumer onStopPreview;
        private final OptionInstance.TooltipSupplier<Integer> tooltipSupplier;
        private final Consumer<Integer> onChanged;
        private boolean smoothDragging;
        private int lastPreviewed;

        PresetSliderButton(int x, int y, int width, int height, OptionInstance<Integer> option,
                           int maxIndex, IntFunction<Component> labelGetter,
                           Runnable onDragStart, IntConsumer onStopPreview,
                           OptionInstance.TooltipSupplier<Integer> tooltipSupplier, Consumer<Integer> onChanged) {
            super(x, y, width, height, labelGetter.apply(option.get()),
                    maxIndex == 0 ? 0 : option.get() / (double) maxIndex);
            this.option = option;
            this.maxIndex = maxIndex;
            this.labelGetter = labelGetter;
            this.onDragStart = onDragStart;
            this.onStopPreview = onStopPreview;
            this.tooltipSupplier = tooltipSupplier;
            this.onChanged = onChanged;
            this.lastPreviewed = option.get();
            this.updateMessage();
        }

        private int nearestIndex() {
            return (int) Math.round(this.value * maxIndex);
        }

        private double sliderPos(int index) {
            return maxIndex == 0 ? 0 : index / (double) maxIndex;
        }

        @Override
        protected void updateMessage() {
            // While the thumb moves freely, the label and tooltip already describe the stop it
            // would snap to. Updating both here (vanilla slider behavior) keeps the tooltip live
            // during drags instead of frozen on the value the screen opened with.
            int index = nearestIndex();
            this.setMessage(labelGetter.apply(index));
            this.setTooltip(tooltipSupplier.apply(index));
        }

        @Override
        protected void applyValue() {
            if (smoothDragging) {
                // Mid-drag: live-preview whenever the thumb crosses onto a different stop.
                int index = nearestIndex();
                if (index != lastPreviewed) {
                    lastPreviewed = index;
                    onStopPreview.accept(index);
                }
            } else {
                // Keyboard / programmatic changes commit immediately.
                commit();
            }
        }

        private void commit() {
            int index = nearestIndex();
            this.value = sliderPos(index);
            this.lastPreviewed = index;
            if (!Objects.equals(option.get(), index)) {
                option.set(index);
                onChanged.accept(index);
            }
            this.updateMessage();
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClicked) {
            this.smoothDragging = true;
            this.lastPreviewed = nearestIndex();
            // Capture the pre-drag mix so sliding back onto Custom can restore it.
            this.onDragStart.run();
            super.onClick(event, doubleClicked);
        }

        @Override
        protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
            this.smoothDragging = true;
            super.onDrag(event, dragX, dragY);
        }

        @Override
        public void onRelease(MouseButtonEvent event) {
            if (this.smoothDragging) {
                this.smoothDragging = false;
                commit();
            }
            super.onRelease(event);
        }

        @Override
        public void resetValue() {
            // re-sync thumb + label from the re-derived option value, never mid-drag
            if (smoothDragging) return;
            int index = option.get();
            this.value = sliderPos(index);
            this.lastPreviewed = index;
            this.updateMessage();
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            // One stop per arrow key press, rather than vanilla's pixel-sized nudge.
            int dir = event.isLeft() ? -1 : event.isRight() ? 1 : 0;
            if (dir != 0) {
                this.value = sliderPos(Mth.clamp(nearestIndex() + dir, 0, maxIndex));
                commit();
                return true;
            }
            return super.keyPressed(event);
        }
    }

}
