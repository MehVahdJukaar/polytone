package net.mehvahdjukaar.polytone.content.block;

import net.mehvahdjukaar.polytone.content.sound.BlockSoundEmitter;

import java.util.List;
import java.util.Map;

//TODO: idk
public class BlockSoundEmitters {


    private Map<TickSource, List<BlockSoundEmitter>> emittersBySource;

    private BlockSoundEmitters(Map<TickSource, List<BlockSoundEmitter>> emittersBySource) {
        this.emittersBySource = emittersBySource;
    }
}
