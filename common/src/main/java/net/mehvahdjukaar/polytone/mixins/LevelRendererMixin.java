package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = LevelRenderer.class, priority = 1300)
public class LevelRendererMixin {

    @Shadow
    @Nullable
    private ClientLevel level;

    @Shadow @Final private Minecraft minecraft;

    @ModifyExpressionValue(method = "renderClouds",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;getCloudHeight()F"))
    private float polytone$modifyCloudHeight(float original) {
        Float f = Polytone.DIMENSION_MODIFIERS.modifyCloudHeight(this.level);
        return f != null ? f : original;
    }

    @ModifyArg(method = "renderLevel",
            at = @At(value = "INVOKE",
                    ordinal = 1,
                    target = "Lnet/minecraft/client/renderer/FogRenderer;setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;Lorg/joml/Vector4f;FZF)Lnet/minecraft/client/renderer/FogParameters;"))
    private Vector4f polytone$modifyTerrainFogColor(Vector4f original, @Local(argsOnly = true) Camera camera,
                                                    @Local(ordinal = 1) float partialTicks,
                                                    @Local(argsOnly = true) GameRenderer gameRenderer) {

        return Polytone.DIMENSION_MODIFIERS.modifyTerrainFogColor(original, this.level,
                camera, partialTicks, gameRenderer, this.minecraft);
    }
}
