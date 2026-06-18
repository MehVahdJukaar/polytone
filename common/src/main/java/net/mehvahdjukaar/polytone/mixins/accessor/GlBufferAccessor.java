package net.mehvahdjukaar.polytone.mixins.accessor;

import com.mojang.blaze3d.opengl.GlBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the raw GL buffer name of a Mojang {@link GlBuffer}. Needed to bind our
 * expression-uniform UBOs to shader programs (e.g. Sodium's chunk program) via raw
 * {@code glBindBufferBase}, since those programs are not driven through Mojang's RenderPass.
 */
@Mixin(GlBuffer.class)
public interface GlBufferAccessor {
    @Accessor("handle")
    int polytone$getHandle();
}
