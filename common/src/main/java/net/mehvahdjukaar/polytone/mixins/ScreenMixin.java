package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.slotify.GuiModifierManager;
import net.mehvahdjukaar.polytone.slotify.ScreenModifier;
import net.mehvahdjukaar.polytone.slotify.SlotifyScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin implements SlotifyScreen {

    @Unique
    private ScreenModifier polytone$modifier = null;

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("TAIL"))
    private  void onInit(CallbackInfo ci) {
        polytone$modifier = GuiModifierManager.getGuiModifier((Screen) (Object) this);
    }

    @Override
    public void polytone$renderExtraSprites(PoseStack poseStack) {
        if (polytone$modifier != null) {
            RenderSystem.enableDepthTest();
            polytone$modifier.sprites().forEach(r -> r.render(poseStack));
        }
    }

    @Override
    public boolean polytone$hasSprites() {
        return polytone$modifier != null && !polytone$modifier.sprites().isEmpty();
    }

    @Override
    public ScreenModifier polytone$getModifier() {
        return polytone$modifier;
    }

    @Inject(method = "addWidget", at = @At("HEAD"))
    public <T extends GuiEventListener & NarratableEntry> void modifyWidget2(T listener, CallbackInfoReturnable<T> cir) {
        if (polytone$modifier != null && listener instanceof AbstractWidget aw) {
            for (var m : polytone$modifier.widgetModifiers()) {
                m.maybeModify(aw);
            }
        }
    }

    @Inject(method = "addRenderableOnly", at = @At("HEAD"))
    public <T extends Renderable> void modifyRenderable(T listener, CallbackInfoReturnable<T> cir) {
        if (polytone$modifier != null && listener instanceof AbstractWidget aw) {
            for (var m : polytone$modifier.widgetModifiers()) {
                m.maybeModify(aw);
            }
        }
    }

}
