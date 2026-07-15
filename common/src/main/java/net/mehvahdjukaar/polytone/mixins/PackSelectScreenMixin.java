package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.common.gui.PointingChatBubbleOverlay;
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

@Mixin(PackSelectionScreen.class)
public abstract class PackSelectScreenMixin extends Screen {

    protected PackSelectScreenMixin(Component component) {
        super(component);
    }

    // RIGHT: append the config button after the Done button (ordinal 3) without shifting the layout.
    @WrapOperation(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
            ordinal = 3))
    public <T extends LayoutElement> T polytone$addButtonRight(LinearLayout footer, T doneButton, Operation<T> original) {
        if (Polytone.CONFIGS.getButtonPos() != ConfigsManager.ButtonPosition.RIGHT) {
            return original.call(footer, doneButton);
        }

        int buttonW = 20;
        int buttonSpacing = 8;
        // Under-reports its width by the config button so centering ignores it and Done stays put.
        LinearLayout centerGroup = new ExtraWidthHorizontalLayout(-buttonW - buttonSpacing, 0)
                .spacing(buttonSpacing);
        centerGroup.defaultCellSetting().alignHorizontallyLeft();

        centerGroup.addChild(doneButton);
        centerGroup.addChild(poly$makeButton(buttonW));

        footer.addChild(centerGroup);
        return doneButton;
    }

    // LEFT: prepend the config button before the first footer button (ordinal 2) without shifting the layout.
    @WrapOperation(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
            ordinal = 2))
    public <T extends LayoutElement> T polytone$addButtonLeft(LinearLayout footer, T firstButton, Operation<T> original) {
        if (Polytone.CONFIGS.getButtonPos() != ConfigsManager.ButtonPosition.LEFT) {
            return original.call(footer, firstButton);
        }

        int buttonW = 20;
        int buttonSpacing = 8;
        LinearLayout centerGroup = new ExtraWidthHorizontalLayout(-buttonW - buttonSpacing,
                -buttonW - buttonSpacing + 2) //no idea why +2 is needed here
                .spacing(buttonSpacing);
        centerGroup.defaultCellSetting().alignHorizontallyLeft();

        centerGroup.addChild(poly$makeButton(buttonW));
        centerGroup.addChild(firstButton);

        footer.addChild(centerGroup);
        return firstButton;
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
    private SpriteIconButton polytone$configButton;
}
