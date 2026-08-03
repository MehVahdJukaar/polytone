package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Remembers the last entity the player interacts with, so expressions can read it via global.lastInteractedEntity
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    // 26.1 folded the old interact/interactAt pair into a single interact() that carries the hit result
    @Inject(method = "interact", at = @At("HEAD"))
    private void polytone$trackInteract(Player player, Entity target, EntityHitResult hitResult, InteractionHand hand,
                                        CallbackInfoReturnable<InteractionResult> cir) {
        ClientFrameTicker.setLastEntity(target);
    }
}
