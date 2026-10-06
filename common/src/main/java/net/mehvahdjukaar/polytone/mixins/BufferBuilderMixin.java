package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.polytone.content.shaders.light.IFaceInLightBuilder;
import net.mehvahdjukaar.polytone.content.shaders.light.LightShaderPatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferBuilder.class)
public class BufferBuilderMixin implements IFaceInLightBuilder {

    //once per builder, chunk builders get remade after a reload anyway
    @Unique
    private boolean polytone$storesFaceInLight = false;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void polytone$checkFaceInLight(ByteBufferBuilder buffer, PrimitiveTopology primitiveTopology, VertexFormat format, CallbackInfo ci) {
        polytone$storesFaceInLight = format == DefaultVertexFormat.BLOCK && LightShaderPatcher.canStoreFaceInLight();
    }

    @Override
    public boolean polytone$storesFaceInLight() {
        return polytone$storesFaceInLight;
    }
}
