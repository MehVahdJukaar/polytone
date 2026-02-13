package net.mehvahdjukaar.polytone.content.config;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static net.minecraft.client.Options.genericValueLabel;

public class ConfigScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("screen.polytone.configs.title");

    private final Multimap<String, OptionHolder<?>> optionsPerCategory = MultimapBuilder.hashKeys().arrayListValues().build();
    private final Runnable saveFunc;

    public ConfigScreen(Screen screen, Collection<OptionHolder<?>> options, Runnable safeFunc) {
        super(screen, Minecraft.getInstance().options, TITLE);
        for (OptionHolder<?> e : options) {
            String cat = e.fileId.getNamespace();
            optionsPerCategory.put(cat, e);
        }
        this.saveFunc = safeFunc;
    }

    public ConfigScreen(Screen scree, Multimap<String, OptionHolder<?>> options, Runnable safeFunc) {
        super(scree, Minecraft.getInstance().options, TITLE);
        this.optionsPerCategory.putAll(options);
        this.saveFunc = safeFunc;
    }

    @Override
    public void removed() {
        saveFunc.run();
    }

    private void resetValues() {
        for (var entry : optionsPerCategory.asMap().entrySet()) {
            for (var option : entry.getValue()) {
                resetOptionValue(option.option);
            }
        }
    }

    private <T> void resetOptionValue(OptionInstance<T> option) {
        if (option.values() instanceof PolyConfig<T> c) {
            option.set(c.getDefaultValue());
        }
        resetOption(option);
    }

    @Override
    protected void addFooter() {
        LinearLayout linearLayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        linearLayout.addChild(Button.builder(Component.translatable("screen.polytone.configs.reset"),
                b -> resetValues()).build());
        linearLayout.addChild(Button.builder(CommonComponents.GUI_DONE,
                b -> this.minecraft.setScreen(this.lastScreen)).build());
    }

    @Override
    protected void addOptions() {
        for (var modId : optionsPerCategory.keySet()) {
            this.list.addHeader(Component.literal(getReadableName(modId)));
            Collection<OptionHolder<?>> options = optionsPerCategory.get(modId);
            OptionInstance<?> presetOpt = makePresetOpt(options, modId);
            if (presetOpt != null) this.list.addBig(presetOpt);
            OptionInstance<?>[] sortedOptions = options.stream()
                    .sorted(Comparator.comparingInt((OptionHolder<?> o) -> {
                                        if (o.option.values() instanceof PolyConfig<?> c) {
                                            return c.getDisplayOrder();
                                        }
                                        return 0;
                                    })
                                    .thenComparing(o -> o.fileId)
                    )
                    .map(o -> o.option)
                    .toArray(OptionInstance<?>[]::new);
            this.list.addSmall(sortedOptions);
        }
    }

    //ugliest code ever
    @Nullable
    private OptionInstance<?> makePresetOpt(Collection<OptionHolder<?>> optHolders, String modId) {
        var known = LAST_KNOWN_PRESET_WIDGETS.get(modId);
        if (known != null) return known;

        Map<String, Runnable> presetActions = new HashMap<>();
        for (OptionHolder<?> holder : optHolders) {
            OptionInstance<?> op = holder.option;
            if (op.values() instanceof PolyConfig<?> c) {
                addPresetActions(presetActions, op);
            }
        }
        if (presetActions.isEmpty()) return null;

        StringConfig valueSet = new StringConfig(Optional.empty(), Map.of(), 0, "none",
                new ArrayList<>(presetActions.keySet()));

        Identifier id = Identifier.fromNamespaceAndPath(modId, "presets");
        var opt = new OptionInstance<>(id.toLanguageKey(),
                OptionInstance.cachedConstantTooltip(
                        Component.translatable(id.withSuffix(".tooltip").toLanguageKey())),
                (component, value) -> genericValueLabel(component,
                        Component.translatable(id.withSuffix("." + value).toLanguageKey())),
                valueSet,
                valueSet.codec(), valueSet.getDefaultValue(), (v) -> {
            presetActions.get(v).run();
            //mega hack. TODO: do better!
            Minecraft.getInstance().setScreen(new ConfigScreen(this.lastScreen, optionsPerCategory, saveFunc));
        }
        );
        LAST_KNOWN_PRESET_WIDGETS.put(modId, opt);
        return opt;
    }

    private <T> void addPresetActions(Map<String, Runnable> presets, OptionInstance<T> option) {
        if (option.values() instanceof PolyConfig<T> c) {
            for (var entry : c.getPresets().entrySet()) {
                Runnable setAction = () -> option.set(entry.getValue());
                presets.merge(entry.getKey(), setAction, (a, b) -> () -> {
                    a.run();
                    b.run();
                });
            }
        }
    }

    private static final Map<String, OptionInstance<String>> LAST_KNOWN_PRESET_WIDGETS = new HashMap<>();

    public static void clearPresetCache() {
        LAST_KNOWN_PRESET_WIDGETS.clear();
    }

    public static String getReadableName(String name) {
        return Arrays.stream((name).replace(":", "_").split("_"))
                .map(StringUtils::capitalize).collect(Collectors.joining(" "));
    }
}
