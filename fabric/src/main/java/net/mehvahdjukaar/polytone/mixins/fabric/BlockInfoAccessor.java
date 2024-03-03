package net.mehvahdjukaar.polytone.mixins.fabric;

import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractBlockRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractBlockRenderContext.class)
public interface BlockInfoAccessor {

    @Accessor("blockInfo")
    BlockRenderInfo getBlockInfo();
}
