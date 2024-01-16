package net.mehvahdjukaar.polytone.mixins.forge;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.fluid.FluidPropertiesManager;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//@Mixin(ForgeHooksClient.class)
public class ForgeHooksClientMixin {
/*
    @ModifyExpressionValue(method = "getFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/extensions/common/IClientFluidTypeExtensions;modifyFogColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IFLorg/joml/Vector3f;)Lorg/joml/Vector3f;"))
    private static Vector3f polytone$modifyWaterFogColor(Vector3f original, @Local FluidState fluidState) {
        return FluidPropertiesManager.modifyFogColor(original, )
    }*/
}
