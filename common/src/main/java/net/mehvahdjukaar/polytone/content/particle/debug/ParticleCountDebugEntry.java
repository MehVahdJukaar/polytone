package net.mehvahdjukaar.polytone.content.particle.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugEntryCategory;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class ParticleCountDebugEntry implements DebugScreenEntry {
    public void display(DebugScreenDisplayer debugScreenDisplayer, @Nullable Level level, @Nullable LevelChunk levelChunk, @Nullable LevelChunk levelChunk2) {
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.getCameraEntity();
        if (entity != null && mc.level != null) {
            BlockPos blockPos = entity.blockPosition();
            if (mc.level.isInsideBuildHeight(blockPos.getY())) {
                debugScreenDisplayer.addLine("Particles: " + mc.particleEngine.countParticles());
            }

        }
    }

    @Override
    public DebugEntryCategory category() {
        return DebugScreenEntry.super.category();
    }
}
