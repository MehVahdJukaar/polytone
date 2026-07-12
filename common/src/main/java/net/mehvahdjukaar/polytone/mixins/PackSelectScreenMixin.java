package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.gui.ChatBubbleWidget;
import net.mehvahdjukaar.polytone.content.config.ConfigsManager;
import net.mehvahdjukaar.polytone.content.config.ExtraWidthHorizontalLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PackSelectionScreen.class)
public abstract class PackSelectScreenMixin extends Screen {

    @Shadow
    @Final
    private HeaderAndFooterLayout layout;

    protected PackSelectScreenMixin(Component component) {
        super(component);
    }

    @Shadow
    public abstract void onClose();

    @WrapOperation(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
            ordinal = 4))
    public <T extends LayoutElement> T polytone$addButtonRight(LinearLayout instance, T doneButton, Operation<T> original) {
        if (Polytone.CONFIGS.getButtonPos() != ConfigsManager.ButtonPosition.RIGHT) {
            return original.call(instance, doneButton);
        }

        int buttonW = 20;
        int buttonSpacing = 8;
        LinearLayout centerGroup = new ExtraWidthHorizontalLayout(-buttonW - buttonSpacing, 0)
                .spacing(buttonSpacing);
        centerGroup.defaultCellSetting().alignHorizontallyLeft();

        centerGroup.addChild(doneButton);
        SpriteIconButton spriteButton = poly$makeButton(buttonW);

        centerGroup.addChild(spriteButton);
        instance.addChild(centerGroup);
        return doneButton;
    }

    @WrapOperation(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
            ordinal = 3))
    public <T extends LayoutElement> T polytone$addButtonLeft(LinearLayout instance, T doneButton, Operation<T> original) {
        if (Polytone.CONFIGS.getButtonPos() != ConfigsManager.ButtonPosition.LEFT) {
            return original.call(instance, doneButton);
        }

        int buttonW = 20;
        int buttonSpacing = 8;
        LinearLayout centerGroup = new ExtraWidthHorizontalLayout(-buttonW - buttonSpacing,
                -buttonW - buttonSpacing + 2) //no idea why +2 is needed here
                .spacing(buttonSpacing);
        centerGroup.defaultCellSetting().alignHorizontallyLeft();

        SpriteIconButton spriteButton = poly$makeButton(buttonW);

        centerGroup.addChild(spriteButton);
        centerGroup.addChild(doneButton);

        instance.addChild(centerGroup);
        return doneButton;
    }

    @Unique
    private @NonNull SpriteIconButton poly$makeButton(int buttonW) {
        SpriteIconButton button = SpriteIconButton.builder(Component.translatable("options.accessibility"),
                        (arg) -> {
                            Polytone.CONFIGS.bubbleManager.onConfigButtonClicked();
                            Minecraft.getInstance().setScreen(
                                    Polytone.CONFIGS.createScreenForPack((PackSelectionScreen) (Screen) this));
                        },
                        true).width(buttonW)
                .sprite(Polytone.res("paint_brush"), 12, 12).build();
        this.polytone$configButton = button;
        return button;
    }

    @Unique
    private @Nullable SpriteIconButton polytone$configButton;
    @Unique
    private final ChatBubbleWidget polytone$bubble =
            new ChatBubbleWidget(0, 0, Component.empty()).setAnimated(true);

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        SpriteIconButton button = this.polytone$configButton;
        if (button == null || !button.visible) return;

        Component message = Polytone.CONFIGS.bubbleManager.getConfigButtonMessage(Polytone.CONFIGS.hasPackConfigs());
        if (message == null) return;

        if (!message.equals(this.polytone$bubble.getMessage())) {
            this.polytone$bubble.setText(message);
        }
        this.polytone$bubble.renderPointingAt(graphics, button, this.width, mouseX, mouseY, partialTick);
    }

}
