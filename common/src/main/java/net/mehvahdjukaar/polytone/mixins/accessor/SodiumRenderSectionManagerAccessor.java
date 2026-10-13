package net.mehvahdjukaar.polytone.mixins.accessor;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.DeferredTaskList;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(RenderSectionManager.class)
public interface SodiumRenderSectionManagerAccessor {

    @Invoker(value = "readRenderListFromTree", remap = false)
    void polytone$readRenderListFromTree(Viewport viewport, FogParameters fogParameters);

    @Accessor(value = "taskLists", remap = false)
    DeferredTaskList polytone$getTaskLists();

    @Accessor(value = "taskLists", remap = false)
    void polytone$setTaskLists(DeferredTaskList taskLists);
}
