package net.mehvahdjukaar.polytone.content.config;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("screen.polytone.configs.title");

    private final Multimap<String, OptionInstance<?>> opt = MultimapBuilder.hashKeys().arrayListValues().build();
    private final Runnable saveFunc;

    public ConfigScreen(Screen screen, Collection<OptionHolder<?>> options, Runnable safeFunc) {
        super(screen, Minecraft.getInstance().options, TITLE);
        for (OptionHolder<?> e : options) {
            String cat = e.fileId.getNamespace();
            opt.put(cat, e.option);
        }
        this.saveFunc = safeFunc;
    }

    @Override
    public void removed() {
        saveFunc.run();
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
        for (var cat : opt.keySet()) {
            this.list.addHeader(Component.literal(getReadableName(cat)));
            if (cat.equals(Polytone.MOD_ID)) {
                List<OptionInstance<?>> options = new ArrayList<>(opt.get(cat));
                options.remove(Polytone.CONFIGS.lenientLoading.option);
                this.list.addBig(Polytone.CONFIGS.lenientLoading.option);
                this.list.addSmall(options.toArray(new OptionInstance[0]));

            } else this.list.addSmall(opt.get(cat).toArray(new OptionInstance[0]));
        }
    }

    public static String getReadableName(String name) {
        return Arrays.stream((name).replace(":", "_").split("_"))
                .map(StringUtils::capitalize).collect(Collectors.joining(" "));
    }
}
