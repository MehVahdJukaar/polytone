package net.mehvahdjukaar.polytone.mixins.accessor;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(PostPass.class)
public interface PostPassAccessor {

    @Accessor("pipeline")
    RenderPipeline polytone$getPipeline();

    @Accessor("inputs")
    List<PostPass.Input> polytone$getInputs();
}
