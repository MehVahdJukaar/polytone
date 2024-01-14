package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.slotify.GuiModifierManager;
import net.mehvahdjukaar.slotify.ScreenModifier;
import net.mehvahdjukaar.slotify.SlotifyScreen;
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
    private ScreenModifier slotify$modifier = null;

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("TAIL"))
    private  void onInit(CallbackInfo ci) {
        slotify$modifier = GuiModifierManager.getGuiModifier((Screen) (Object) this);
    }

    @Override
    public void slotify$renderExtraSprites(PoseStack poseStack) {
        if (slotify$modifier != null) {
            RenderSystem.enableDepthTest();
            slotify$modifier.sprites().forEach(r -> r.render(poseStack));
        }
    }

    @Override
    public boolean slotify$hasSprites() {
        return slotify$modifier != null && !slotify$modifier.sprites().isEmpty();
    }

    @Override
    public ScreenModifier slotify$getModifier() {
        return slotify$modifier;
    }

    @Inject(method = "addWidget", at = @At("HEAD"))
    public <T extends GuiEventListener & NarratableEntry> void modifyWidget2(T listener, CallbackInfoReturnable<T> cir) {
        if (slotify$modifier != null && listener instanceof AbstractWidget aw) {
            for (var m : slotify$modifier.widgetModifiers()) {
                m.maybeModify(aw);
            }
        }
    }

    @Inject(method = "addRenderableOnly", at = @At("HEAD"))
    public <T extends Renderable> void modifyRenderable(T listener, CallbackInfoReturnable<T> cir) {
        if (slotify$modifier != null && listener instanceof AbstractWidget aw) {
            for (var m : slotify$modifier.widgetModifiers()) {
                m.maybeModify(aw);
            }
        }
    }

}
