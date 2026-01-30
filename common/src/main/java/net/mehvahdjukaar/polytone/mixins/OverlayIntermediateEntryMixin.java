package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.ConditionUtils;
import net.mehvahdjukaar.polytone.common.PolyConditionalOverlay;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.util.TriState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(OverlayMetadataSection.OverlayEntry.IntermediateEntry.class)
public class OverlayIntermediateEntryMixin implements PolyConditionalOverlay {


    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;create(Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"))
    private static Codec<OverlayMetadataSection.OverlayEntry.IntermediateEntry> polytone$decorateCodec(
            Codec<OverlayMetadataSection.OverlayEntry.IntermediateEntry> original) {
        return ConditionUtils.decorate(original);
    }

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
}
