package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
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

    @Inject(method = "interactAt", at = @At("HEAD"))
    private void polytone$trackInteractAt(Player player, Entity target, EntityHitResult ray, InteractionHand hand,
                                          CallbackInfoReturnable<InteractionResult> cir) {
        ClientFrameTicker.setLastEntity(target);
    }

    @Inject(method = "interact", at = @At("HEAD"))
    private void polytone$trackInteract(Player player, Entity target, InteractionHand hand,
                                        CallbackInfoReturnable<InteractionResult> cir) {
        ClientFrameTicker.setLastEntity(target);
    }
}
