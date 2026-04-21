package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.common.PolyConditionalOverlay;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.util.TriState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(OverlayMetadataSection.OverlayEntry.class)
public class OverlayEntryMixin implements PolyConditionalOverlay {
    @Unique
    private TriState polytone$condition = TriState.DEFAULT;

    @Override
    public void polytone$setCondition(TriState triState) {
        this.polytone$condition = triState;
    }

    @Override
    public TriState polytone$getCondition() {
        return this.polytone$condition;
    }

    @ModifyReturnValue(method = "isApplicable", at = @At(value = "RETURN"))
    private boolean polytone$overrideIsApplicable(boolean original) {
        return switch (this.polytone$getCondition()) {
            case TRUE -> true;
            case FALSE -> false;
            default -> original;
        };
    }


    @ModifyReturnValue(method = {"lambda$listCodecForPackType$0","method_72312"},
            at = @At(value = "RETURN"))
    private static OverlayMetadataSection.OverlayEntry polytone$decodeListWithPolytoneCodec(
            OverlayMetadataSection.OverlayEntry original,
            @Local(argsOnly = true) OverlayMetadataSection.OverlayEntry.IntermediateEntry intermediate) {
        ((PolyConditionalOverlay) (Object) original).polytone$setCondition(
                ((PolyConditionalOverlay) (Object) intermediate).polytone$getCondition());

        return original;
    }


}
