package net.mehvahdjukaar.polytone.content.config;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.common.gui.PointingChatBubbleOverlay;
import net.mehvahdjukaar.polytone.compat.nautilus.PolytoneNautilus;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import static net.minecraft.client.Options.genericValueLabel;

public class ConfigScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("screen.polytone.configs.title");
    // gap before the impact line, matching the 4px tooltip image margins
    private static final int IMPACT_GAP = 4;
    // Where the "install the editor" button sends users when Nautilus Studio isn't present.
    private static final String NAUTILUS_URL = "https://github.com/MehVahdJukaar/pack_editor";

    private final Multimap<String, OptionHolder<?>> opt = MultimapBuilder.linkedHashKeys().arrayListValues().build();
    private final Set<String> collapsed = new HashSet<>();
    private final Runnable safeFunc;

    @Nullable
    private SpriteIconButton heartButton;
    @Nullable
    private EditorIconButton editorButton;
    private boolean rebuildScheduled;
    private volatile boolean editorBooting;

    // Shadows OptionsSubScreen.layout (which is final and built once in the constructor). That
    // inherited layout is never emptied, so re-running the vanilla init on every namespace toggle
    // (via rebuildWidgets) keeps re-appending the list/footer/title to it and visitWidgets then
    // re-registers every stale copy - the "widgets pile up on collapse" bug. We instead run our
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

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderCustomTooltip(guiGraphics, mouseX, mouseY);
    }

    // --- footer ---

    @Override
    protected void addFooter() {
        int iconW = 20;
        // One centered footer row: [editor] Reset Undo Done [heart]. The 20px icons on each end
        // roughly balance, so the Reset/Undo/Done buttons read as centered. Text-button widths are
        // sized so the whole row fits the vanilla footer.
        int textBtnW = Mth.positiveCeilDiv(150 * 2 - 8, 3);
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));

        // Nautilus Studio pack-editor button (left) - always shown. With the editor mod present it
        // opens the editor; without it, its tooltip explains and a click opens the download page.
        boolean editorAvailable = PlatStuff.isModLoaded("nautilus_studio");
        EditorIconButton editor = new EditorIconButton(iconW, 20,
                Component.translatable("screen.polytone.editor.open"),
                editorAvailable, this,
                b -> onEditorPressed(editorAvailable));
        editor.setTooltip(Tooltip.create(Component.translatable(
                editorAvailable ? "screen.polytone.editor.open" : "screen.polytone.editor.no_mod")));
        this.editorButton = footer.addChild(editor);
        footer.addChild(Button.builder(Component.translatable("screen.polytone.configs.reset"),
                b -> resetValues()).width(textBtnW).build());
        footer.addChild(Button.builder(Component.translatable("screen.polytone.configs.undo"),
                b -> undoValues()).width(textBtnW).build());
        footer.addChild(Button.builder(CommonComponents.GUI_DONE,
                b -> this.minecraft.setScreen(this.lastScreen)).width(textBtnW).build());
        // Support/heart button (right).
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
        // Arrange our shadow layout (mirrors OptionsSubScreen#repositionElements, but on our layout).
        this.layout.arrangeElements();
        if (this.list != null) {
            this.list.updateSize(this.width, this.layout);
        }
    }

    private void onEditorPressed(boolean available) {
        if (available) {
            openEditor();
            return;
        }
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

    // --- reset / undo ---

    private void resetValues() {
        for (OptionHolder<?> holder : opt.values()) {
            holder.resetToDefault();
        }
        rebuildPreservingScroll();
    }

    private void undoValues() {
        for (OptionHolder<?> holder : opt.values()) {
            holder.undoChanges();
        }
        rebuildPreservingScroll();
    }

    /**
     * Option buttons are value snapshots, so a programmatic change (reset/undo/preset) needs a
     * rebuild to refresh their labels. Rebuild is cheap and duplicate-free (fresh layout each init);
     * we only restore the scroll position so the list doesn't jump.
     */
    private void rebuildPreservingScroll() {
        double scroll = this.list != null ? this.list.getScrollAmount() : 0;
        this.rebuildWidgets();
        if (this.list != null) {
            this.list.setScrollAmount(scroll);
        }
    }

    /**
     * Defers a rebuild to the end of the frame. Used by the preset slider: rebuilding synchronously
     * inside a widget's own mouse-release handler would free the widget mid-dispatch.
     */
    private void scheduleRebuild() {
        if (rebuildScheduled) return;
        rebuildScheduled = true;
        this.minecraft.execute(() -> {
            rebuildScheduled = false;
            if (this.minecraft.screen == this) rebuildPreservingScroll();
        });
    }

    // --- list population ---

    @Override
    protected void addOptions() {
        for (String modId : opt.keySet()) {
            boolean expanded = !collapsed.contains(modId);
            addNamespaceHeader(getCategoryHeader(modId), expanded, modId);
            if (!expanded) continue;

            List<OptionHolder<?>> sorted = opt.get(modId).stream()
                    .sorted(Comparator.comparingInt(ConfigScreen::displayOrderOf)
                            .thenComparing(o -> o.fileId))
                    .toList();

            // Group entries by section, keeping display_order within each; section placement below.
            Map<Optional<String>, List<OptionHolder<?>>> bySection = new LinkedHashMap<>();
            for (OptionHolder<?> holder : sorted) {
                bySection.computeIfAbsent(sectionOf(holder), k -> new ArrayList<>()).add(holder);
            }

            // "presets" feeds the pack-wide slider at the top of the namespace, "section_presets"
            // the slider of the entry's own section.
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

            addPresetSlider(modId, overall, null);

            for (Optional<String> section : orderedSections(bySection)) {
                List<OptionHolder<?>> group = bySection.get(section);
                if (group == null || group.isEmpty()) continue;
                section.ifPresent(s -> {
                    addSectionHeader(sectionTitle(modId, s));
                    addPresetSlider(modId, sectionSliders.get(section), s);
                });
                addOptionRows(group);
            }
        }
    }

    private void addNamespaceHeader(Component title, boolean expanded, String modId) {
        // Clickable namespace header (chevron + bold title); toggling rebuilds the list.
        NamespaceHeaderWidget header = new NamespaceHeaderWidget(
                this.list.getRowWidth(), 20, title, expanded, b -> toggleNamespace(modId));
        this.list.addSmall(List.<AbstractWidget>of(header));
    }

    private void addSectionHeader(Component title) {
        StringWidget widget = new StringWidget(this.list.getRowWidth(), 20,
                title.copy().withStyle(ChatFormatting.GRAY), this.font);
        widget.alignLeft();
        this.list.addSmall(List.<AbstractWidget>of(widget));
    }

    private void addOptionRows(List<OptionHolder<?>> group) {
        List<OptionInstance<?>> pending = new ArrayList<>();
        for (OptionHolder<?> holder : group) {
            boolean wide = holder.option.values() instanceof PolyConfig<?> c && c.isWide();
            if (wide) {
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

    // --- header title resolution ---

    private static Component getCategoryHeader(String modId) {
        String key = "config." + modId + ".header";
        if (I18n.exists(key)) return Component.translatable(key);
        return Component.literal(getReadableName(modId));
    }

    private static Component sectionTitle(String modId, String section) {
        String key = "config." + modId + ".section." + section;
        if (I18n.exists(key)) return Component.translatable(key);
        return Component.literal(getReadableName(section));
    }

    // --- rich tooltip (preview image + performance impact) ---

    private void renderCustomTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.list == null) return;

        for (OptionHolder<?> holder : opt.values()) {
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
            // image in the middle, chosen by the option's current value
            PolyConfig.TooltipImage image = config.getTooltipImages().get(String.valueOf(holder.get()));
            boolean hasImage = image != null;
            if (hasImage) {
                components.add(new ClientImageTooltip(image.texture(), image.width(), image.height()));
            }
            // performance impact (bottom); a small spacer when no image already supplies the gap
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
                guiGraphics.renderTooltipInternal(this.font, components, mouseX, mouseY,
                        DefaultTooltipPositioner.INSTANCE);
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
        public int getHeight() {
            return height;
        }
    }

    // --- preset sliders ---

    private void addPresetSlider(String modId, @Nullable Map<String, List<PresetAction<?>>> presets,
                                 @Nullable String section) {
        if (presets == null || presets.isEmpty()) return;
        this.list.addBig(makePresetOpt(presets, modId, section));
    }

    private OptionInstance<?> makePresetOpt(Map<String, List<PresetAction<?>>> presets, String modId,
                                            @Nullable String section) {
        List<String> names = List.copyOf(presets.keySet());

        // Every option any preset touches; used to snapshot/restore for the Custom stop.
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

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(modId, "presets");
        String titleKey = id.toLanguageKey();
        // Per-section title override -> pack-wide title -> generic "Preset".
        Component caption = firstTranslated(
                section == null ? null : titleKey + ".section." + section, titleKey);

        // General slider tooltip, used as the fallback for stops without their own.
        Component fallbackTooltip = null;
        if (section != null) fallbackTooltip = translatedOrNull(titleKey + ".section." + section + ".tooltip");
        if (fallbackTooltip == null) fallbackTooltip = translatedOrNull(titleKey + ".tooltip");
        net.minecraft.client.gui.components.Tooltip[] stopTooltips =
                new net.minecraft.client.gui.components.Tooltip[names.size() + 1];
        for (int i = 0; i < stopTooltips.length; i++) {
            Component t = presetTooltip(modId, section, i, names, fallbackTooltip);
            stopTooltips[i] = t == null ? null : net.minecraft.client.gui.components.Tooltip.create(t);
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
        // Applies a stop's values, then rebuilds so the affected option widgets refresh.
        IntConsumer applyStop = index -> {
            if (index >= names.size()) {
                customSnapshot.forEach(Runnable::run);
            } else {
                presets.get(names.get(index)).forEach(PresetAction::apply);
            }
            scheduleRebuild();
        };

        return new OptionInstance<>(titleKey,
                tooltipSupplier,
                (component, index) -> genericValueLabel(caption, presetValueLabel(modId, section, index, names)),
                new PresetSliderValueSet(names.size(),
                        index -> genericValueLabel(caption, presetValueLabel(modId, section, index, names)),
                        onDragStart, applyStop),
                current, index -> {});
    }

    private static <T> Runnable captureRestore(OptionInstance<T> option) {
        T value = option.get();
        return () -> option.set(value);
    }

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
        if (section != null) {
            Component c = translatedOrNull(modId + ".presets.section." + section + "." + name);
            if (c != null) return c;
        }
        Component c = translatedOrNull(modId + ".presets." + name);
        return c != null ? c : Component.literal(getReadableName(name));
    }

    @Nullable
    private static Component translatedOrNull(String key) {
        return I18n.exists(key) ? Component.translatable(key) : null;
    }

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

    // ValueSet whose widget is the snap-to-stop slider below, mirroring vanilla's graphics Preset
    // slider: free thumb while dragging, snap + apply on release. Position is derived from values.
    private record PresetSliderValueSet(int maxIndex, IntFunction<Component> labelGetter,
                                        Runnable onDragStart, IntConsumer onStopCommit)
            implements OptionInstance.ValueSet<Integer> {

        @Override
        public Function<OptionInstance<Integer>, AbstractWidget> createButton(
                OptionInstance.TooltipSupplier<Integer> tooltipSupplier, Options options,
                int x, int y, int width, Consumer<Integer> onChanged) {
            return optionInstance -> new PresetSliderButton(x, y, width, 20,
                    optionInstance, maxIndex, labelGetter, onDragStart, onStopCommit, tooltipSupplier);
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

    private static final class PresetSliderButton extends AbstractSliderButton {
        private final OptionInstance<Integer> option;
        private final int maxIndex;
        private final IntFunction<Component> labelGetter;
        private final Runnable onDragStart;
        private final IntConsumer onStopCommit;
        private final OptionInstance.TooltipSupplier<Integer> tooltipSupplier;
        private boolean smoothDragging;

        PresetSliderButton(int x, int y, int width, int height, OptionInstance<Integer> option,
                           int maxIndex, IntFunction<Component> labelGetter, Runnable onDragStart,
                           IntConsumer onStopCommit, OptionInstance.TooltipSupplier<Integer> tooltipSupplier) {
            super(x, y, width, height, labelGetter.apply(option.get()),
                    maxIndex == 0 ? 0 : option.get() / (double) maxIndex);
            this.option = option;
            this.maxIndex = maxIndex;
            this.labelGetter = labelGetter;
            this.onDragStart = onDragStart;
            this.onStopCommit = onStopCommit;
            this.tooltipSupplier = tooltipSupplier;
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
            // The label and tooltip describe the stop the thumb would snap to, live during a drag.
            int index = nearestIndex();
            this.setMessage(labelGetter.apply(index));
            this.setTooltip(tooltipSupplier.apply(index));
        }

        @Override
        protected void applyValue() {
            // No live apply: preset values are committed on release / keyboard commit only.
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            this.smoothDragging = true;
            // Capture the pre-drag mix so sliding onto Custom can restore it.
            this.onDragStart.run();
            super.onClick(mouseX, mouseY);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            this.smoothDragging = true;
            super.onDrag(mouseX, mouseY, dragX, dragY);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            if (this.smoothDragging) {
                this.smoothDragging = false;
                commit();
            }
            super.onRelease(mouseX, mouseY);
        }

        private void commit() {
            int index = nearestIndex();
            this.value = sliderPos(index);
            if (!Objects.equals(option.get(), index)) {
                option.set(index);
                onStopCommit.accept(index);
            }
            this.updateMessage();
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            // One stop per arrow key press, rather than vanilla's pixel-sized nudge.
            int dir = keyCode == InputConstants.KEY_LEFT ? -1 : keyCode == InputConstants.KEY_RIGHT ? 1 : 0;
            if (dir != 0) {
                this.value = sliderPos(Mth.clamp(nearestIndex() + dir, 0, maxIndex));
                commit();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public static String getReadableName(String name) {
        return Arrays.stream(name.replace(":", "_").split("_"))
                .map(StringUtils::capitalize).collect(Collectors.joining(" "));
    }

    /**
     * Icon-only footer button that swaps its icon with the editor state: the animated "loading"
     * sprite while the editor window boots off-thread, an "on" sprite once it is open, and the plain
     * sprite otherwise (also the fixed icon when the editor mod isn't installed). Subclasses Button
     * directly rather than SpriteIconButton because the latter's sprite field is final.
     */
    private static final class EditorIconButton extends Button {
        private static final ResourceLocation SPRITE = Polytone.res("codec_editor");
        private static final ResourceLocation SPRITE_ON = Polytone.res("codec_editor_on");
        private static final ResourceLocation SPRITE_LOADING = Polytone.res("codec_editor_loading");
        private static final int ICON = 16;

        private final ConfigScreen screen;
        private final boolean available;

        EditorIconButton(int width, int height, Component message, boolean available,
                         ConfigScreen screen, OnPress onPress) {
            super(0, 0, width, height, message, onPress, DEFAULT_NARRATION);
            this.available = available;
            this.screen = screen;
        }

        private ResourceLocation icon() {
            // Guard on availability before touching PolytoneEditor: with nautilus_studio absent that
            // class must never load.
            if (!available) return SPRITE;
            if (screen.editorBooting) return SPRITE_LOADING;
            return PolytoneNautilus.isOpen() ? SPRITE_ON : SPRITE;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            int x = this.getX() + this.getWidth() / 2 - ICON / 2;
            int y = this.getY() + this.getHeight() / 2 - ICON / 2;
            guiGraphics.blitSprite(this.icon(), x, y, ICON, ICON);
        }

        @Override
        public void renderString(GuiGraphics guiGraphics, Font font, int color) {
            // Icon-only: no label.
        }
    }
}
