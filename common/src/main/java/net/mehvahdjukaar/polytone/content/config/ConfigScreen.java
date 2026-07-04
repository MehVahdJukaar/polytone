package net.mehvahdjukaar.polytone.content.config;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.gui.PointingChatBubbleOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.StringWidget;
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
import java.util.List;
import java.util.stream.Collectors;

public class ConfigScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("screen.polytone.configs.title");

    private final Multimap<String, OptionInstance<?>> opt = MultimapBuilder.hashKeys().arrayListValues().build();
    private final Runnable safeFunc;

    @Nullable
    private SpriteIconButton heartButton;

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
                    () -> Polytone.CONFIGS.bubbleManager.getHeartButtonMessage()));
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
        LinearLayout linearLayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        linearLayout.addChild(Button.builder(Component.translatable("screen.polytone.configs.reset"),
                b -> resetValues()).build());
        linearLayout.addChild(Button.builder(CommonComponents.GUI_DONE,
                b -> this.minecraft.setScreen(this.lastScreen)).build());
        SpriteIconButton heart = SpriteIconButton.builder(
                        Component.translatable("screen.polytone.support.title"),
                        b -> this.minecraft.setScreen(new SupportScreen(this)),
                        true)
                .size(20, 20)
                .sprite(Polytone.res("heart"), 16, 16)
                .build();
        this.heartButton = heart;
        linearLayout.addChild(heart);
    }

    @Override
    protected void addOptions() {
        for (var cat : opt.keySet()) {
            // 1.21.1's OptionsList has no addHeader, so we add a full-width centered label row as a category header
            this.list.addSmall(List.<AbstractWidget>of(
                    new StringWidget(this.list.getRowWidth(), 20, getCategoryHeader(cat), this.font).alignCenter()));
            this.list.addSmall(opt.get(cat).toArray(new OptionInstance[0]));
        }
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
