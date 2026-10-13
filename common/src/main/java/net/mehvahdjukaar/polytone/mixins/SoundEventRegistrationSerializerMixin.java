package net.mehvahdjukaar.polytone.mixins;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.common.ConditionUtils;
import net.mehvahdjukaar.polytone.common.struc.TriState;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.resources.sounds.SoundEventRegistrationSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(SoundEventRegistrationSerializer.class)
public class SoundEventRegistrationSerializerMixin {

    @ModifyReturnValue(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/resources/sounds/SoundEventRegistration;",
            at = @At("RETURN"))
    private SoundEventRegistration polytone$applyPolyCondition(SoundEventRegistration original, @Local(argsOnly = true) JsonElement json) {
        TriState condition = ConditionUtils.CODEC_OVERLAY.parse(JsonOps.INSTANCE, json).getOrThrow();
        if (condition != TriState.FALSE) return original;
        //hoping this empty doesnt cause issues
        return new SoundEventRegistration(List.of(), false, null);
    }
}
