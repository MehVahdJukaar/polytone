package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives {@link net.mehvahdjukaar.polytone.content.entity.EntityModifiersManager#onEntityTick} once
 * per client-side entity tick. Done as a common mixin (rather than a loader tick event) so both
 * NeoForge and Fabric go through the same path.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void polytone$onEntityTick(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!self.level().isClientSide) return;
        Polytone.ENTITY_MODIFIERS.onEntityTick(self);
    }
}
