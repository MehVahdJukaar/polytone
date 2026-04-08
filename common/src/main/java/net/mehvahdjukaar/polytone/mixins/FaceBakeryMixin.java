package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.math.Transformation;
import net.mehvahdjukaar.polytone.utils.IVariantExtension;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FaceBakery.class)
public class FaceBakeryMixin {

    @WrapWithCondition(method = "bakeQuad", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/model/FaceBakery;recalculateWinding([ILnet/minecraft/core/Direction;)V"))
    public boolean poly$allowArbitraryRot(FaceBakery instance, int[] vertices, Direction direction,
                                          @Local(argsOnly = true) ModelState transform) {
        var t = transform.getRotation();
        if (t == Transformation.identity()) return true;
        if (transform instanceof IVariantExtension variant) {
            return !variant.poly$isUncapped();
        }
        return true;
    }

}
