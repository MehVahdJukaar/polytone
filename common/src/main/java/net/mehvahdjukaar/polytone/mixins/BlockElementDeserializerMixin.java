package net.mehvahdjukaar.polytone.mixins;

import com.google.gson.JsonObject;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.util.GsonHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockElement.Deserializer.class)
public class BlockElementDeserializerMixin {


    @Inject(method = "getAngle", at = @At("HEAD"), cancellable = true)
    public void polytone$unrestrictAngles(JsonObject json, CallbackInfoReturnable<Float> cir){
        float f = GsonHelper.getAsFloat(json, "angle");
        cir.setReturnValue(f);
    }
}
