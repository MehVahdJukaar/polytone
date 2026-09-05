package net.mehvahdjukaar.polytone.content.config;

import net.mehvahdjukaar.polytone.common.StrUtils;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import static net.minecraft.client.Options.genericValueLabel;

final class ConfigPresets {

    private final Map<String, Runnable> rederivers = new LinkedHashMap<>();
    private final Consumer<Collection<OptionInstance<?>>> refresher;
    private boolean suppress;

    ConfigPresets(Consumer<Collection<OptionInstance<?>>> refresher) {
        this.refresher = refresher;
    }

    void clear() {
        rederivers.clear();
    }

    void rederiveAll() {
        if (!suppress) rederivers.values().forEach(Runnable::run);
    }

    void runSuppressed(Runnable action) {
        suppress = true;
        try {
            action.run();
        } finally {
            suppress = false;
        }
    }

    OptionInstance<?> makeOption(Map<String, List<Action<?>>> presets, String modId,
                                 @Nullable String section) {
        String rederiveKey = modId + "/" + (section == null ? "" : section);
        List<String> names = List.copyOf(presets.keySet());

        Set<OptionInstance<?>> affected = new LinkedHashSet<>();
        for (var actions : presets.values()) {
            for (var action : actions) affected.add(action.option());
        }

        IntSupplier derive = () -> {
            for (int i = 0; i < names.size(); i++) {
                if (presets.get(names.get(i)).stream().allMatch(Action::matches)) {
                    return i;
                }
            }
            return names.size();
        };
        int current = derive.getAsInt();

        Identifier id = Identifier.fromNamespaceAndPath(modId, "presets");
        String titleKey = id.toLanguageKey();
        Component caption = firstTranslated(
                section == null ? null : titleKey + ".section." + section,
                titleKey);

        Component fallbackTooltip = null;
        if (section != null) fallbackTooltip = translatedOrNull(titleKey + ".section." + section + ".tooltip");
        if (fallbackTooltip == null) fallbackTooltip = translatedOrNull(titleKey + ".tooltip");
        Tooltip[] stopTooltips = new Tooltip[names.size() + 1];
        for (int i = 0; i < stopTooltips.length; i++) {
            Component t = presetTooltip(modId, section, i, names, fallbackTooltip);
            stopTooltips[i] = t == null ? null : Tooltip.create(t);
        }
        OptionInstance.TooltipSupplier<Integer> tooltipSupplier =
                index -> (index >= 0 && index < stopTooltips.length) ? stopTooltips[index] : null;

        List<Runnable> customSnapshot = new ArrayList<>();
        Runnable onDragStart = () -> {
            customSnapshot.clear();
            for (OptionInstance<?> option : affected) {
                customSnapshot.add(captureRestore(option));
            }
        };
        IntConsumer applyStop = index -> {
            suppress = true;
            try {
                if (index >= names.size()) {
                    customSnapshot.forEach(Runnable::run);
                } else {
                    presets.get(names.get(index)).forEach(Action::apply);
                }
            } finally {
                suppress = false;
            }
            refresher.accept(affected);
            rederiveOthersExcept(rederiveKey);
        };

        var opt = new OptionInstance<>(titleKey,
                tooltipSupplier,
                (component, index) -> genericValueLabel(caption, presetValueLabel(modId, section, index, names)),
                new PresetSlider.PresetSliderValueSet(names.size(),
                        index -> genericValueLabel(caption, presetValueLabel(modId, section, index, names)),
                        onDragStart, applyStop),
                current, (index) -> {
            if (index < names.size()) {
                applyStop.accept(index);
            }
        });

        rederivers.put(rederiveKey, () -> {
            int index = derive.getAsInt();
            if (!Objects.equals(opt.get(), index)) {
                suppress = true;
                try {
                    opt.set(index);
                } finally {
                    suppress = false;
                }
            }
            refresher.accept(List.of(opt));
        });
        return opt;
    }
    private static <T> Runnable captureRestore(OptionInstance<T> option) {
        T value = option.get();
        return () -> option.set(value);
    }

    private void rederiveOthersExcept(String exceptKey) {
        rederivers.forEach((key, rederiver) -> {
            if (!key.equals(exceptKey)) rederiver.run();
        });
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
        return Component.translatableWithFallback(modId + ".presets." + name,
                StrUtils.readableName(name));
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

    static <T> void collect(Map<String, List<Action<?>>> presets,
                                                 OptionInstance<T> option, boolean sectionScoped) {
        if (option.values() instanceof PolyConfig<T> c) {
            Map<String, T> declared = sectionScoped ? c.getSectionPresets() : c.getPresets();
            for (var entry : declared.entrySet()) {
                presets.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .add(new Action<>(option, entry.getValue()));
            }
        }
    }

    record Action<T>(OptionInstance<T> option, T value) {
        void apply() {
            option.set(value);
        }

        boolean matches() {
            return option.get().equals(value);
        }
    }
}
