package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.ClassUtils;

public abstract class PositionalProxy {

    protected final BlockPos pos;
    protected final Level level;

    private BlockState state;
    private BlockEntity be;
    private Holder<Biome> biome;

    public PositionalProxy(Level level, BlockPos pos, BlockState state) {
        this.level = level;
        this.pos = pos;
        this.state = state;
    }

    public PositionalProxy(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    protected BlockState getStateInternal() {
        if (state == null) {
            state = level.getBlockState(pos);
        }
        return state;
    }

    protected Holder<Biome> getBiomeInternal() {
        if (biome == null) {
            biome = level.getBiome(pos);
        }
        return biome;
    }

    protected BlockEntity getBlockEntityInternal() {
        if (be == null && this.hasBlockEntity()) {
            be = level.getBlockEntity(pos);
        }
        return be;
    }

    public String id() {
        return getStateInternal().getBlockHolder().getRegisteredName();
    }

    public String state() {
        return getStateInternal().toString();
    }

    public Object stateValue(Object input) {
        //return a String or primitive value representing the state property and turn the input into the best known state prop
        Property<?> property = getStateInternal().getBlock().getStateDefinition().getProperty(input.toString());
        var value = getStateInternal().getValue(property);
        if (ClassUtils.isPrimitiveOrWrapper(value.getClass())) {
            return value;
        }
        return value.toString();
    }

    public String biome() {
        return getBiomeInternal().getRegisteredName();
    }

    public double temperature() {
        Holder<Biome> biome = getBiomeInternal();
        return ColorUtils.getClimateSettings(biome.value()).temperature;
    }

    public double downfall() {
        Holder<Biome> biome = getBiomeInternal();
        return ColorUtils.getClimateSettings(biome.value()).downfall;
    }

    public int skyLight() {
        return level.getBrightness(LightLayer.SKY, pos);
    }

    public int blockLight() {
        return level.getBrightness(LightLayer.BLOCK, pos);
    }

    public boolean canSeeSky() {
        return level.canSeeSky(pos);
    }

    public boolean hasEntitiesWithin(){
        return !level.getEntities(null, state.getShape(level, pos).bounds().move(pos)).isEmpty();
    }

    public boolean hasTag(String tag){
        TagKey<Block> tagKey = TagKey.create(Registries.BLOCK,  Identifier.parse(tag));
        return getStateInternal().is(tagKey);
    }

    public boolean isAir(){
        return getStateInternal().isAir();
    }

    public boolean hasLiquid(){
        return !getStateInternal().getFluidState().isEmpty();
    }

    public boolean hasBlockEntity() {
        return getStateInternal().hasBlockEntity();
    }

    public String blockEntity(){
        var be = getBlockEntityInternal();
        if (be != null) {
            return be.getType().builtInRegistryHolder().getRegisteredName();
        }
        return "null";
    }

}
