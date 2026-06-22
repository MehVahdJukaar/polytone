package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.utils.ConditionUtils;
import net.mehvahdjukaar.polytone.utils.PolyConditionalOverlay;
import net.mehvahdjukaar.polytone.utils.TriState;
import net.minecraft.server.packs.OverlayMetadataSection;
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

    @ModifyReturnValue(method = "isApplicable", at = @At("RETURN"))
    private boolean polytone$overrideIsApplicable(boolean original) {
        return switch (this.polytone$condition) {
            case TRUE -> true;
            case FALSE -> false;
            default -> original;
        };
    }

    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE",
            target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;create(Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"))
    private static Codec<OverlayMetadataSection.OverlayEntry> polytone$decorateCodec(
            Codec<OverlayMetadataSection.OverlayEntry> original) {
        return ConditionUtils.decorate(original);
    }
}
