package net.mehvahdjukaar.polytone.common.expressions.proxies;

import com.google.common.base.Preconditions;
import net.mehvahdjukaar.candlelight.api.BeanGettersAliases;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.ClassUtils;
import org.jspecify.annotations.NonNull;

//with cache but position changing awareness
@BeanGettersAliases
public abstract class PositionalProxy {

    private BlockState stateCache;
    private BlockEntity beCache;
    private Holder<Biome> biomeCache;
    private BlockPos posCache;

    public PositionalProxy(BlockState state) {
        this.stateCache = state;
    }

    public PositionalProxy() {
    }

    protected abstract Level getLevelInternal();

    protected abstract BlockPos getPosInternal();

    private BlockPos updatedPos() {
        BlockPos newPos = getPosInternal();
        if (newPos == posCache) return posCache;
        if (posCache == null || !posCache.equals(newPos)) {
            posCache = newPos;
            //invalidate caches
            stateCache = null;
            beCache = null;
            biomeCache = null;
        }
        return posCache;
    }

    protected BlockState getStateInternal() {
        BlockPos pos = updatedPos(); //refresh cache if needed
        if (stateCache == null) {
            stateCache = getLevelInternal().getBlockState(pos);
        }
        return stateCache;
    }

    protected Holder<Biome> getBiomeInternal() {
        BlockPos blockPos = updatedPos();
        if (biomeCache == null) {
            biomeCache = getLevelInternal().getBiome(blockPos);
        }
        return biomeCache;
    }

    protected BlockEntity getBlockEntityInternal() {
        BlockPos blockPos = updatedPos();
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
        return getLevelInternal().getBrightness(LightLayer.SKY, updatedPos());
    }

    public int blockLight() {
        return getLevelInternal().getBrightness(LightLayer.BLOCK, updatedPos());
    }

    public boolean canSeeSky() {
        return getLevelInternal().canSeeSky(updatedPos());
    }

    public boolean hasEntitiesWithin() {
        Level level = getLevelInternal();
        BlockPos pos = updatedPos();
        return !level.getEntities(null, getStateInternal().getShape(level, pos).bounds().move(pos)).isEmpty();
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
        var be = getBlockEntityInternal();
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

            EnvironmentAttribute<?> attr = parseEnvAttr(attributeName);
            // safe to call delegate methods now
            return getLevelInternal().environmentAttributes()
                    .getValue(Preconditions.checkNotNull(attr), getPosInternal());

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
