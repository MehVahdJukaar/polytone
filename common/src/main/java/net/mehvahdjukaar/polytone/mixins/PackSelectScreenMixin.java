package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.gui.PointingChatBubbleOverlay;
import net.mehvahdjukaar.polytone.content.config.ConfigsManager;
import net.mehvahdjukaar.polytone.content.config.ExtraWidthHorizontalLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(PackSelectionScreen.class)
public abstract class PackSelectScreenMixin extends Screen {

    protected PackSelectScreenMixin(Component component) {
        super(component);
    }

    @WrapOperation(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
            ordinal = 3))
    public <T extends LayoutElement> T polytone$addConfigButton(LinearLayout instance, T doneButton, Operation<T> original) {
        ConfigsManager.ButtonPosition pos = Polytone.CONFIGS.getButtonPos();
        if (pos == ConfigsManager.ButtonPosition.NONE) {
            return original.call(instance, doneButton);
        }

        int buttonW = 20;
        int buttonSpacing = 8;
        int extraX = pos == ConfigsManager.ButtonPosition.LEFT ? -buttonW - buttonSpacing + 2 : 0;
        LinearLayout centerGroup = new ExtraWidthHorizontalLayout(-buttonW - buttonSpacing, extraX)
                .spacing(buttonSpacing);
        centerGroup.defaultCellSetting().alignHorizontallyLeft();

        SpriteIconButton configButton = poly$makeButton(buttonW);
        if (pos == ConfigsManager.ButtonPosition.LEFT) {
            centerGroup.addChild(configButton);
            centerGroup.addChild(doneButton);
        } else {
            centerGroup.addChild(doneButton);
            centerGroup.addChild(configButton);
        }

        instance.addChild(centerGroup);
        return doneButton;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void polytone$addConfigBubbleOverlay(CallbackInfo ci) {
        SpriteIconButton button = this.polytone$configButton;
        if (button == null) return;

        this.addRenderableOnly(new PointingChatBubbleOverlay(
                button,
                () -> this.width,
                () -> Polytone.CONFIGS.bubbleManager.getConfigButtonMessage(Polytone.CONFIGS.hasPackConfigs())));
    }

    @Unique
    private SpriteIconButton poly$makeButton(int buttonW) {
        SpriteIconButton button = SpriteIconButton.builder(Component.translatable("screen.polytone.configs.title"),
                        b -> {
                            Polytone.CONFIGS.bubbleManager.onConfigButtonClicked();
                            Minecraft.getInstance().setScreen(
                                    Polytone.CONFIGS.createScreen((PackSelectionScreen) (Object) this));
                        },
                        true).width(buttonW)
                .sprite(Polytone.res("paint_brush"), 16, 16).build();
        this.polytone$configButton = button;
        return button;
    }

    @Unique
    private @Nullable SpriteIconButton polytone$configButton;
}
