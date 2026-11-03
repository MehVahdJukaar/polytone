package net.mehvahdjukaar.polytone.common.expressions.proxies;

import com.google.common.base.Preconditions;
import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.expressions.ExpUtils;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
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
import org.jspecify.annotations.Nullable;

//with cache but position changing awareness
//all these nulls all because of color getters possibly having a null tint getter propagating till here...
@BeanAliases
public abstract class PositionalProxy {

    private BlockState stateCache;
    private BlockEntity beCache;
    private Biome biomeCache;
    private Holder<Biome> biomeHolderCache;
    private BlockPos posCache;


    public PositionalProxy(@Nullable BlockState state, @Nullable Biome biome) {
        this.stateCache = state;
        this.biomeCache = biome;
    }

    public PositionalProxy(@Nullable BlockState state, @Nullable Holder<Biome> biome) {
        this.stateCache = state;
        if (biome != null) {
            this.biomeCache = biome.value();
            this.biomeHolderCache = biome;
        }
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

    public abstract double x();

    public abstract double y();

    public abstract double z();

    public double distSq(PositionalProxy other) {
        double dx = x() - other.x();
        double dy = y() - other.y();
        double dz = z() - other.z();
        return dx * dx + dy * dy + dz * dz;
    }

    public double dist(PositionalProxy other) {
        return Math.sqrt(distSq(other));
    }

    @Nullable
    private BlockPos updatedPos() {
        BlockPos newPos = getPosInternal();
        if (newPos == posCache) return posCache;
        if (posCache == null || !posCache.equals(newPos)) {
            //first call keeps whatever the constructor seeded (attribute blending hands us a neighbour biome)
            if (posCache != null) {
                stateCache = null;
                beCache = null;
                biomeCache = null;
                biomeHolderCache = null;
            }
            posCache = newPos == null ? null : newPos.immutable();
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
            biomeHolderCache = holder;
        }
        return biomeCache;
    }

    @Nullable
    protected Holder<Biome> getBiomeHolderInternal() {
        Biome biome = getBiomeInternal();
        if (biome == null) return null;
        if (biomeHolderCache == null) {
            //not the best
            Level level = Minecraft.getInstance().level;
            if (level == null) return null;
            biomeHolderCache = level.registryAccess().lookupOrThrow(Registries.BIOME).wrapAsHolder(biome);
        }
        return biomeHolderCache;
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
        return getStateInternal().typeHolder().getRegisteredName();
    }

    public String blockState() {
        return getStateInternal().toString();
    }

    public Object blockStateValue(Object input) {
        BlockState state = getStateInternal();
        Property<?> property = state.getBlock().getStateDefinition().getProperty(input.toString());
        if (property == null) return false;
        var value = state.getValue(property);
        if (ClassUtils.isPrimitiveOrWrapper(value.getClass())) {
            return value;
        }
        return value.toString();
    }

    public String biome() {
        var holder = getBiomeHolderInternal();
        return holder == null ? null : holder.getRegisteredName();
    }

    public double biomeIndex() {
        return 1 - BiomeIdMapper.LEGACY.getIndex(getBiomeInternal());
    }

    public double biomeIndex(String biomeMapper) {
        BiomeIdMapper mapper = Polytone.BIOME_ID_MAPPERS.get(biomeMapper);
        if (mapper == null) {
            throw new IllegalArgumentException("Unknown biome mapper: " + biomeMapper);
        }
        return 1 - mapper.getIndex(getBiomeInternal());
    }

    public double temperature() {
        return ColorUtils.getClimateSettings(getBiomeInternal()).temperature();
    }

    public double downfall() {
        return ColorUtils.getClimateSettings(getBiomeInternal()).downfall();
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
        return getLevelInternal().getBrightness(LightLayer.BLOCK, blockPos);
    }

    public boolean canSeeSky() {
        BlockPos blockPos = updatedPos();
        var l = getLevelInternal();
        if (blockPos == null || l == null) return true;
        return getLevelInternal().canSeeSky(blockPos);
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
        TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, Identifier.parse(tag));
        return getStateInternal().is(tagKey);
    }

    public boolean hasBiomeTag(String tag) {
        var holder = getBiomeHolderInternal();
        return holder != null && holder.is(TagKey.create(Registries.BIOME, Identifier.parse(tag)));
    }

    public boolean hasAirAt() {
        return getStateInternal().isAir();
    }

    public boolean hasFluid() {
        return !getStateInternal().getFluidState().isEmpty();
    }

    public String fluid() {
        return BuiltInRegistries.FLUID.getKey(getStateInternal().getFluidState().getType()).toString();
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
            // recursion detected - return default or null
            return null; //this will crash but somebody shouldn't try to get an attribute from within an attribute expression
        }

        try {
            inEnvironmentAttributeCall = true;
            BlockPos pos = getPosInternal();
            if (pos == null) {
                pos = BlockPos.ZERO;
            }
            EnvironmentAttribute<?> attr = ExpUtils.parseEnvAttr(attributeName);
            // safe to call delegate methods now

            LevelReader lr;
            if (getLevelInternal() instanceof LevelReader lrr) {
                lr = lrr;
            } else {
                lr = Minecraft.getInstance().level;
                if (lr == null) {
                    throw new IllegalStateException("No level available for environment attribute retrieval!");
                }
            }
            return lr.environmentAttributes()
                    .getValue(Preconditions.checkNotNull(attr), pos);

        } finally {
            inEnvironmentAttributeCall = false;
        }
    }


}
