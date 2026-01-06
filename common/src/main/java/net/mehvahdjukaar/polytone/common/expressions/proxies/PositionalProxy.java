package net.mehvahdjukaar.polytone.common.expressions.proxies;

import com.google.common.base.Preconditions;
import net.mehvahdjukaar.candlelight.api.BeanGettersAliases;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.ClassUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

//with cache but position changing awareness
@BeanGettersAliases
public abstract class PositionalProxy {

    private BlockState stateCache;
    private BlockEntity beCache;
    private Biome biomeCache;
    private String biomeNameCache;
    private BlockPos posCache;

    public PositionalProxy(@Nullable BlockState state) {
        this.stateCache = state;
    }

    public PositionalProxy() {
    }

    protected abstract LevelReader getLevelInternal();

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
            stateCache = pos == null ? Blocks.AIR.defaultBlockState() :
                    getLevelInternal().getBlockState(pos);
        }
        return stateCache;
    }

    protected Biome getBiomeInternal() {
        BlockPos blockPos = updatedPos();
        if (biomeCache == null) {
            var bb = blockPos == null ? getLevelInternal().registryAccess()
                    .lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS) :
                    getLevelInternal().getBiome(blockPos);
            biomeCache = bb.value();
            biomeNameCache = bb.getRegisteredName();
        }
        return biomeCache;
    }

    @Nullable
    protected BlockEntity getBlockEntityInternal() {
        BlockPos blockPos = updatedPos();
        if (blockPos == null) return null;
        if (beCache == null && this.hasBlockEntity()) {
            beCache = getLevelInternal().getBlockEntity(blockPos);
        }
        return beCache;
    }

    public String block() {
        return getStateInternal().getBlockHolder().getRegisteredName();
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
        if (biomeNameCache == null) {
            if (biomeCache != null) {
                biomeNameCache = getLevelInternal().registryAccess()
                        .lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)
                        .getRegisteredName();
            } else getBiomeInternal(); //updates cache
        }
        return biomeNameCache;
    }

    public float biomeIndex() {
        return 1 - BiomeIdMapper.LEGACY.getIndex(getBiomeInternal());
    }

    public float biomeIndex(String biomeMapper) {
        BiomeIdMapper mapper = Polytone.BIOME_ID_MAPPERS.get(biomeMapper);
        if (mapper == null) {
            throw new IllegalArgumentException("Unknown biome mapper: " + biomeMapper);
        }
        return 1 - mapper.getIndex(getBiomeInternal());
    }

    public double temperature() {
        return ColorUtils.getClimateSettings(getBiomeInternal()).temperature;
    }

    public double downfall() {
        return ColorUtils.getClimateSettings(getBiomeInternal()).downfall;
    }

    public int skyLight() {
        BlockPos blockPos = updatedPos();
        if (blockPos == null) return 15;
        return getLevelInternal().getBrightness(LightLayer.SKY, blockPos);
    }

    public int blockLight() {
        BlockPos blockPos = updatedPos();
        if (blockPos == null) return 0;
        return getLevelInternal().getBrightness(LightLayer.BLOCK, blockPos);
    }

    public boolean canSeeSky() {
        BlockPos blockPos = updatedPos();
        if (blockPos == null) return true;
        return getLevelInternal().canSeeSky(blockPos);
    }

    public boolean hasEntitiesWithin() {
        LevelReader level = getLevelInternal();
        BlockPos pos = updatedPos();
        if (pos == null) return false;
        if (level instanceof Level l) {
            return !l.getEntities(null, getStateInternal().getShape(level, pos).bounds().move(pos)).isEmpty();
        }
        return false;
    }


    public boolean hasBlockTag(String tag) {
        TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, Identifier.parse(tag));
        return getStateInternal().is(tagKey);
    }

    public boolean isAir() {
        return getStateInternal().isAir();
    }

    public boolean hasFluid() {
        return !getStateInternal().getFluidState().isEmpty();
    }

    public boolean fluid() {
        return !getStateInternal().getFluidState().isEmpty();
    }

    public boolean hasBlockEntity() {
        return getStateInternal().hasBlockEntity();
    }

    public String blockEntity() {
        BlockEntity be = getBlockEntityInternal();
        if (be != null) {
            return be.getType().builtInRegistryHolder().getRegisteredName();
        }
        return "null";
    }

    private boolean inEnvironmentAttributeCall = false;

    public Object environmentAttribute(String attributeName) {
        if (inEnvironmentAttributeCall) {
            // recursion detected — return default or null
            return null; //this will crash but somebody shouldn't try to get an attribute from within an attribute expression
        }

        try {
            inEnvironmentAttributeCall = true;
            BlockPos pos = getPosInternal();
            if (pos == null) {
                pos = BlockPos.ZERO;
            }
            EnvironmentAttribute<?> attr = parseEnvAttr(attributeName);
            // safe to call delegate methods now

            return getLevelInternal().environmentAttributes()
                    .getValue(Preconditions.checkNotNull(attr), pos);

        } finally {
            inEnvironmentAttributeCall = false;
        }
    }

    protected static @NonNull EnvironmentAttribute<?> parseEnvAttr(String attributeName) {
        EnvironmentAttribute<?> attr = BuiltInRegistries.ENVIRONMENT_ATTRIBUTE.getValue(Identifier.parse(attributeName));
        if (attr == null) {
            throw new IllegalArgumentException("Unknown environment attribute: " + attributeName);
        }
        return attr;
    }
}
