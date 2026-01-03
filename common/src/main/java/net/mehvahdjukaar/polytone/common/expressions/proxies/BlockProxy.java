package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanGetters;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.ClassUtils;

@BeanGetters
public class BlockProxy {

    private final BlockState state;
    private final BlockPos pos;
    private final Level level;

    public BlockProxy(Level level, BlockPos pos, BlockState state) {
        this.level = level;
        this.pos = pos;
        this.state = state;
    }

    public String block() {
        return state.getBlockHolder().getRegisteredName();
    }

    public Object stateValue(Object input) {
        //return a String or primitive value representing the state property and turn the input into the best known state prop
        Property<?> property = state.getBlock().getStateDefinition().getProperty(input.toString());
        var value = state.getValue(property);
        if (ClassUtils.isPrimitiveOrWrapper(value.getClass())) {
            return value;
        }
        return value.toString();
    }

    public int x(){
        return pos.getX();
    }

    public int y() {
        return pos.getY();
    }

    public int z() {
        return pos.getZ();
    }

    public BlockPos pos() {
        return pos;
    }

    public String biome(){
        return level.getBiome(pos).getRegisteredName();
    }
}
