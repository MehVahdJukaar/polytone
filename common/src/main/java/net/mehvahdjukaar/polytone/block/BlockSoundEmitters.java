package net.mehvahdjukaar.polytone.block;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.sound.BlockSoundEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: idk
public class BlockSoundEmitters {


    private Map<TickSource, List<BlockSoundEmitter>> emittersBySource;

    private BlockSoundEmitters(Map<TickSource, List<BlockSoundEmitter>> emittersBySource) {
        this.emittersBySource = emittersBySource;
    }
}
