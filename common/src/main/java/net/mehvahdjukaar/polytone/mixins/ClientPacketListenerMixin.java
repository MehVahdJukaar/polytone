package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    //block entity data doesnt go through sendBlockUpdated
    @Inject(method = "handleBlockEntityData(Lnet/minecraft/network/protocol/game/ClientboundBlockEntityDataPacket;)V", at = @At("TAIL"))
    private void polytone$updateVoxelVolumeBlockEntity(ClientboundBlockEntityDataPacket packet, CallbackInfo ci) {
        Polytone.VOXEL_VOLUME.onBlockEntityDataPacket(packet.getPos(), packet.getType());
    }
}
