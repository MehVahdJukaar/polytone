package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackSelectionScreen.class)
public abstract class PackSelectScreenMixin extends Screen {

    protected PackSelectScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void polytone$addConfigButton(CallbackInfo ci) {
        if (Polytone.CONFIGS.isEmpty()) return;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.polytone.configs.title"),
                        b -> this.minecraft.setScreen(Polytone.CONFIGS.createScreen((PackSelectionScreen) (Object) this)))
                .bounds(8, 6, 100, 20).build());
    }
}
