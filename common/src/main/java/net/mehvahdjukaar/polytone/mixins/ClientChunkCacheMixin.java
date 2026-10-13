package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Consumer;

// vanilla reuses the LevelChunk on a resend, so hook where the data changes
@Mixin(ClientChunkCache.class)
public class ClientChunkCacheMixin {

    @Inject(method = "replaceWithPacketData", at = @At("RETURN"))
    private void polytone$surfaceMapChunkLoaded(int x, int z, FriendlyByteBuf buf,
                                                Map<Heightmap.Types, long[]> heightmaps,
                                                Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer,
                                                CallbackInfoReturnable<LevelChunk> cir) {
        Polytone.SURFACE_MAP.markChunkDirty(x, z);
    }

    @Inject(method = "replaceBiomes", at = @At("RETURN"))
    private void polytone$surfaceMapBiomesReplaced(int x, int z, FriendlyByteBuf buf, CallbackInfo ci) {
        Polytone.SURFACE_MAP.markChunkDirty(x, z);
    }
}
