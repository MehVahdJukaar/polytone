package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.math.Transformation;
import net.mehvahdjukaar.polytone.utils.IVariantExtension;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Variant.class)
public class VariantMixin implements  IVariantExtension {

    @Unique
    private boolean poly$uncapped;

    @Override
    public boolean poly$isUncapped() {
        return poly$uncapped;
    }

    @Override
    public void poly$setUncapped(boolean uncapped) {
        poly$uncapped = uncapped;
    }
}
