package net.mehvahdjukaar.polytone.compat.platform;

import net.mehvahdjukaar.polytone.compat.FabricSeasonsCompat;
import net.mehvahdjukaar.polytone.compat.ISeason;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class FabricSeasonsCompatImpl {
    public static float getSeasonNumber(Level level) {
        return 0;
    }

    public static ISeason getSeason(Level arg0) {
        return ISeason.SPRING;
    }

}
