package net.mehvahdjukaar.polytone.mixins.fabric;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ItemBlockRenderTypes.class)
public interface ItemBlockRenderTypeAccessor {

    @Accessor("TYPE_BY_BLOCK")
    static Map<Block, RenderType> getTypeByBlock() {
        return null;
    }
}
