package net.mehvahdjukaar.polytone.content.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.ClassUtils;
import org.jetbrains.annotations.Nullable;

//with cache but position changing awareness
//all these nulls all because of color getters possibly having a null tint getter propagating till here...
@BeanAliases
public abstract class PositionalProxy {

    private BlockState stateCache;
    private BlockEntity beCache;
    private Biome biomeCache;
    private String biomeNameCache;
    private BlockPos posCache;


    public PositionalProxy(@Nullable BlockState state, @Nullable Biome biome) {
        this.stateCache = state;
        this.biomeCache = biome;
    }

    public PositionalProxy(@Nullable BlockState state) {
        this.stateCache = state;
    }

    public PositionalProxy() {
    }

    @Nullable
    protected abstract BlockAndTintGetter getLevelInternal();

    @Nullable
    protected abstract BlockPos getPosInternal();

    @Nullable
    private BlockPos updatedPos() {
        BlockPos newPos = getPosInternal();
        if (newPos == posCache) return posCache;
        if (posCache == null || !posCache.equals(newPos)) {
            posCache = newPos;
            //invalidate caches
            stateCache = null;
            beCache = null;
            biomeCache = null;
            biomeNameCache = null;
        }
        return posCache;
    }

    protected BlockState getStateInternal() {
        BlockPos pos = updatedPos(); //refresh cache if needed
        if (stateCache == null) {
            var l = getLevelInternal();
            stateCache = (pos == null || l == null) ? Blocks.AIR.defaultBlockState() :
                    l.getBlockState(pos);
        }
        return stateCache;
    }

    @Nullable
    protected Biome getBiomeInternal() {
        BlockPos blockPos = updatedPos();
        if (biomeCache == null) {
            var l = getLevelInternal();
            if (!(l instanceof LevelReader lr) || blockPos == null) return null;
            var holder = lr.getBiome(blockPos);
            biomeCache = holder.value();
            biomeNameCache = holder.getRegisteredName();
        }
        return biomeCache;
    }

    @Nullable
    protected BlockEntity getBlockEntityInternal() {
        BlockPos blockPos = updatedPos();
        if (blockPos == null) return null;
        var l = getLevelInternal();
        if (l == null) return null;
        if (beCache == null && this.hasBlockEntity()) {
            beCache = l.getBlockEntity(blockPos);
        }
        return beCache;
    }

    public String block() {
        Block b = getStateInternal().getBlock();
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(b);
        return key == null ? "[unregistered]" : key.toString();
    }

    public String blockState() {
        return getStateInternal().toString();
    }

    public Object blockStateValue(Object input) {
        //return a String or primitive value representing the state property and turn the input into the best known state prop
        Property<?> property = getStateInternal().getBlock().getStateDefinition().getProperty(input.toString());
        var value = getStateInternal().getValue(property);
        if (ClassUtils.isPrimitiveOrWrapper(value.getClass())) {
            return value;
        }
        return value.toString();
    }

    public String biome() {
        updatedPos();

        if (biomeNameCache == null) {
            if (biomeCache != null) {
                if (!(getLevelInternal() instanceof Level)) {
                    biomeNameCache = "plains";
                }
            } else getBiomeInternal(); //updates cache
        }
        return biomeNameCache;
    }

    public double biomeIndex() {
        return 1 - BiomeIdMapper.BY_INDEX.getIndex(getBiomeInternal());
    }

    public double biomeIndex(String biomeMapper) {
        BiomeIdMapper mapper = Polytone.BIOME_ID_MAPPERS.get(biomeMapper);
        if (mapper == null) {
            throw new IllegalArgumentException("Unknown biome mapper: " + biomeMapper);
        }
        return 1 - mapper.getIndex(getBiomeInternal());
    }

    public double temperature() {
        Biome biome = getBiomeInternal();
        return biome == null ? 0 : ColorUtils.getClimateSettings(biome).temperature();
    }

    public double downfall() {
        Biome biome = getBiomeInternal();
        return biome == null ? 0 : ColorUtils.getClimateSettings(biome).downfall();
    }

    public double skyLight() {
        BlockPos blockPos = updatedPos();
        var l = getLevelInternal();
        if (blockPos == null || l == null) return 15;
        return l.getBrightness(LightLayer.SKY, blockPos);
    }

    public double blockLight() {
        BlockPos blockPos = updatedPos();
        var l = getLevelInternal();
        if (blockPos == null || l == null) return 0;
        return l.getBrightness(LightLayer.BLOCK, blockPos);
    }

    public boolean canSeeSky() {
        BlockPos blockPos = updatedPos();
        var l = getLevelInternal();
        if (blockPos == null || l == null) return true;
        return l.canSeeSky(blockPos);
    }

    public boolean hasEntitiesWithin() {
        var level = getLevelInternal();
        BlockPos pos = updatedPos();
        if (pos == null) return false;
        if (level instanceof Level l) {
            return !l.getEntities(null, getStateInternal().getShape(level, pos).bounds().move(pos)).isEmpty();
        }
        return false;
    }


    public boolean hasBlockTag(String tag) {
        TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, ResourceLocation.parse(tag));
        return getStateInternal().is(tagKey);
    }

    public boolean hasAirAt() {
        return getStateInternal().isAir();
    }

    public boolean hasFluid() {
        return !getStateInternal().getFluidState().isEmpty();
    }

    public String fluid() {
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(getStateInternal().getFluidState().getType());
        return key == null ? "[unregistered]" : key.toString();
    }

    public boolean hasBlockEntity() {
        return getStateInternal().hasBlockEntity();
    }

    public String blockEntity() {
        BlockEntity be = getBlockEntityInternal();
        if (be != null) {
            ResourceLocation key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
            return key == null ? "null" : key.toString();
        }
        return "null";
    }

    // Stub - EnvironmentAttribute system doesn't exist on 1.21.1
    public Object environmentAttribute(String attributeName) {
        return 0;
    }


}
