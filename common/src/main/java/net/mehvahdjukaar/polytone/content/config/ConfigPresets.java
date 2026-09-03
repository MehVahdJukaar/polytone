package net.mehvahdjukaar.polytone.content.config;

import net.mehvahdjukaar.polytone.utils.StrUtils;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import static net.minecraft.client.Options.genericValueLabel;

class ConfigPresets {

    private final Map<String, List<Action<?>>> byName = new LinkedHashMap<>();
    private final String modId;
    @Nullable
    private final String section;

    ConfigPresets(String modId, @Nullable String section) {
        this.modId = modId;
        this.section = section;
    }

    <T> void collect(OptionInstance<T> option, boolean sectionScoped) {
        if (!(option.values() instanceof PolyConfig<T> c)) return;
        Map<String, T> declared = sectionScoped ? c.getSectionPresets() : c.getPresets();
        for (var e : declared.entrySet()) {
            byName.computeIfAbsent(e.getKey(), k -> new ArrayList<>())
                    .add(new Action<>(option, e.getValue()));
        }
    }

    boolean isEmpty() {
        return byName.isEmpty();
    }

    OptionInstance<Integer> makeOption(Runnable onApplied) {
        List<String> names = List.copyOf(byName.keySet());
        Component named = translate("");
        Component caption = named != null ? named : Component.translatable("polytone.preset");

        IntFunction<Component> label = index -> genericValueLabel(caption, labelFor(index, names));

        Tooltip[] tooltips = new Tooltip[names.size() + 1];
        for (int i = 0; i < tooltips.length; i++) {
            Component t = tooltipFor(i, names);
            tooltips[i] = t == null ? null : Tooltip.create(t);
        }

        //snapshotted options at drag start so Custom can put them back
        Set<OptionInstance<?>> affected = new LinkedHashSet<>();
        for (var actions : byName.values()) {
            for (var action : actions) affected.add(action.option());
        }
        List<Runnable> snapshot = new ArrayList<>();
        Runnable onDragStart = () -> {
            snapshot.clear();
            for (OptionInstance<?> option : affected) snapshot.add(captureRestore(option));
        };

        PresetSlider values = new PresetSlider(names.size(), label, onDragStart, index -> {
            if (index >= names.size()) snapshot.forEach(Runnable::run);
            else byName.get(names.get(index)).forEach(Action::apply);
            onApplied.run();
        });

        return new OptionInstance<>(key(""),
                index -> index >= 0 && index < tooltips.length ? tooltips[index] : null,
                (component, index) -> label.apply(index),
                values, matchingPreset(names), index -> {});
    }

    private int matchingPreset(List<String> names) {
        for (int i = 0; i < names.size(); i++) {
            if (byName.get(names.get(i)).stream().allMatch(Action::matches)) return i;
        }
        return names.size();
    }

    private Component labelFor(int index, List<String> names) {
        if (index >= names.size()) {
            Component c = translate(".custom");
            return c != null ? c : Component.translatable("polytone.preset.custom");
        }
        String name = names.get(index);
        Component c = translate("." + name);
        return c != null ? c : Component.literal(StrUtils.readableName(name));
    }

    @Nullable
    private Component tooltipFor(int index, List<String> names) {
        String name = index >= names.size() ? "custom" : names.get(index);
        Component c = translate("." + name + ".tooltip");
        return c != null ? c : translate(".tooltip");
    }

    @Nullable
    private Component translate(String suffix) {
        if (section != null) {
            Component c = translatedOrNull(key(".section." + section + suffix));
            if (c != null) return c;
        }
        return translatedOrNull(key(suffix));
    }

    private String key(String suffix) {
        return modId + ".presets" + suffix;
    }

    @Nullable
    private static Component translatedOrNull(String key) {
        return I18n.exists(key) ? Component.translatable(key) : null;
    }

    private static <T> Runnable captureRestore(OptionInstance<T> option) {
        T value = option.get();
        return () -> option.set(value);
    }

    private record Action<T>(OptionInstance<T> option, T value) {
        void apply() {
            option.set(value);
        }

        boolean matches() {
            return option.get().equals(value);
        }
    }
}
