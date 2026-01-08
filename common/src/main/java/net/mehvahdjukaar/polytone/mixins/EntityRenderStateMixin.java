package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.content.entity.IRenderStateWithId;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements IRenderStateWithId {

    @Unique
    private int polytone$id;

    @Override
    public void polytone$setId(int id) {
        polytone$id = id;
    }

    @Override
    public int polytone$getId() {
        return polytone$id;
    }
}
