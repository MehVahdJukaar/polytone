package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanGettersAliases;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.ClassUtils;

@BeanGettersAliases
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

    public String state() {
        return state.toString();
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

    public int x() {
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

    public String biome() {
        return level.getBiome(pos).getRegisteredName();
    }

    public int skyLight() {
        return level.getBrightness(LightLayer.SKY, pos);
    }

    public int blockLight() {
        return level.getBrightness(LightLayer.BLOCK, pos);
    }

    public boolean inValidBounds() {
        return level.isInValidBounds(pos);
    }

    public boolean inWorldBounds() {
        return level.isInWorldBounds(pos);
    }

}
