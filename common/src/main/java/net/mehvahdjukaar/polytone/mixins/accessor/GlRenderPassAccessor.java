package net.mehvahdjukaar.polytone.mixins.accessor;

import com.mojang.blaze3d.opengl.GlRenderPipeline;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// 26.1's frontend RenderPass delegates to a package-private GlRenderPass backend. This accessor exposes that
// backend's compiled pipeline so we can read the program's declared uniform/UBO block names (see
// RenderPassMixin) - targeted by string since the class is package-private and can't be imported.
@Mixin(targets = "com.mojang.blaze3d.opengl.GlRenderPass")
public interface GlRenderPassAccessor {
    @Accessor("pipeline")
    @Nullable
    GlRenderPipeline polytone$getPipeline();
}
