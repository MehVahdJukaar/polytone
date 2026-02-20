package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeStorage.class)
public class SubmitNodeStorageMixin {

    @Inject(method = "submitModel", at = @At("HEAD"))
    private <S> void polytone$onSubmitModel(Model<? super S> model, S object, PoseStack poseStack,
                                                                      RenderType renderType, int i, int j, int k,
                                                                      @Nullable TextureAtlasSprite textureAtlasSprite,
                                                                      int l, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay,
                                                                      CallbackInfo ci) {
        //Cant use forge events as they are missing the camera state

        Polytone.ENTITY_MODIFIERS.onEntityRender(model , poseStack, object);
    }
}
