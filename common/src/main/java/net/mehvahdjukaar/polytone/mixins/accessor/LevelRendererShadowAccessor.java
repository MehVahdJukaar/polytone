package net.mehvahdjukaar.polytone.mixins.accessor;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererShadowAccessor {

    @Accessor("viewArea")
    @Nullable
    ViewArea polytone$getViewArea();

    @Accessor("sectionRenderDispatcher")
    @Nullable
    SectionRenderDispatcher polytone$getSectionRenderDispatcher();
}
