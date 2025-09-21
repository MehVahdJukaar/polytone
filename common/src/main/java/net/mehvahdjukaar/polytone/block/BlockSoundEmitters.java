package net.mehvahdjukaar.polytone.block;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.sound.BlockSoundEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockSoundEmitters {

    public static final Codec<BlockSoundEmitter> LEGACY_CODEC = BlockSoundEmitter.CODEC.listOf().xmap(
            list ->{
                var map = new HashMap<TickSource, BlockSoundEmitter>();
                
            }
    )

    private Map<TickSource, List<BlockSoundEmitter>> emittersBySource;

    private BlockSoundEmitters(Map<TickSource, List<BlockSoundEmitter>> emittersBySource) {
        this.emittersBySource = emittersBySource;
    }
}
