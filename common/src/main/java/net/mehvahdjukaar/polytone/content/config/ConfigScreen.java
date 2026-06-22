package net.mehvahdjukaar.polytone.content.config;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.gui.ChatBubbleWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.Collection;

public class ConfigScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("screen.polytone.configs.title");

    private final Multimap<String, OptionInstance<?>> opt = MultimapBuilder.hashKeys().arrayListValues().build();
    private final Runnable safeFunc;

    @Nullable
    private SpriteIconButton heartButton;
    @Nullable
    private ChatBubbleWidget supportBubble;

    public ConfigScreen(Screen screen, Collection<OptionHolder<?>> options, Runnable safeFunc) {
        super(screen, Minecraft.getInstance().options, TITLE);
        for (OptionHolder<?> e : options) {
            opt.put(e.fileId.getNamespace(), e.option);
        }
        this.safeFunc = safeFunc;
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.heartButton == null || !this.heartButton.visible) return;

        Component message = Polytone.CONFIGS.bubbleManager.getHeartButtonMessage();
        if (message == null) return;

        if (this.supportBubble == null) {
            this.supportBubble = new ChatBubbleWidget(0, 0, message).setAnimated(true);
        } else if (!message.equals(this.supportBubble.getMessage())) {
            this.supportBubble.setText(message);
        }
        this.supportBubble.renderPointingAt(graphics, this.heartButton, this.width, mouseX, mouseY, partialTick);
    }

    @Override
    protected void addOptions() {
        for (var cat : opt.keySet()) {
            this.list.addSmall(opt.get(cat).toArray(new OptionInstance[0]));
        }
    }
}
