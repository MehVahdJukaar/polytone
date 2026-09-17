package net.mehvahdjukaar.polytone.mixins.accessor;

import com.mojang.blaze3d.opengl.GlBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlBuffer.class)
public interface GlBufferAccessor {
    @Accessor("handle")
    int polytone$getHandle();
}
