package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.ITargetProvider;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class ParticleModifier implements ITargetProvider {

    public static final Codec<ParticleModifier> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            Filter.CODEC.optionalFieldOf("filter").forGetter(p -> Optional.ofNullable(p.filter)),
            Colormap.CODEC.optionalFieldOf("colormap").forGetter(p -> Optional.ofNullable(p.colormap)),
            ParticleContextExpression.CODEC.optionalFieldOf("color").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            ParticleContextExpression.CODEC.optionalFieldOf("life").forGetter(p -> Optional.ofNullable(p.lifeGetter)),
            ParticleContextExpression.CODEC.optionalFieldOf("size").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            ParticleContextExpression.CODEC.optionalFieldOf("red").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            ParticleContextExpression.CODEC.optionalFieldOf("green").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            ParticleContextExpression.CODEC.optionalFieldOf("blue").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            ParticleContextExpression.CODEC.optionalFieldOf("alpha").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            ParticleContextExpression.CODEC.optionalFieldOf("speed").forGetter(p -> Optional.ofNullable(p.speedGetter)),
            TARGET_CODEC.optionalFieldOf("targets", Set.of()).forGetter(p -> p.explicitTargets)

    ).apply(instance, ParticleModifier::new));

    @Nullable
    private final Filter filter;
    @Nullable
    public final IColorGetter colormap;
    @Nullable
    public final ParticleContextExpression colorGetter;
    @Nullable
    public final ParticleContextExpression lifeGetter;
    @Nullable
    public final ParticleContextExpression sizeGetter;
    @Nullable
    public final ParticleContextExpression speedGetter;
    @Nullable
    public final ParticleContextExpression redGetter;
    @Nullable
    public final ParticleContextExpression blueGetter;
    @Nullable
    public final ParticleContextExpression greenGetter;
    @Nullable
    public final ParticleContextExpression alphaGetter;
    public final Set<ResourceLocation> explicitTargets;

    private ParticleModifier(Optional<Filter> filter, Optional<IColorGetter> colormap,
                             Optional<ParticleContextExpression> color, Optional<ParticleContextExpression> life,
                             Optional<ParticleContextExpression> size, Optional<ParticleContextExpression> red,
                             Optional<ParticleContextExpression> green, Optional<ParticleContextExpression> blue,
                             Optional<ParticleContextExpression> alpha, Optional<ParticleContextExpression> speed,
                             Set<ResourceLocation> explicitTargets) {
        this(filter.orElse(null), colormap.orElse(null), color.orElse(null), life.orElse(null), size.orElse(null),
                red.orElse(null), green.orElse(null), blue.orElse(null),
                alpha.orElse(null), speed.orElse(null), explicitTargets);
    }

    public ParticleModifier(@Nullable Filter filter, @Nullable IColorGetter colormap,
                            @Nullable ParticleContextExpression color, @Nullable ParticleContextExpression life,
                            @Nullable ParticleContextExpression size, @Nullable ParticleContextExpression red,
                            @Nullable ParticleContextExpression green, @Nullable ParticleContextExpression blue,
                            @Nullable ParticleContextExpression alpha, @Nullable ParticleContextExpression speed,
                            Set<ResourceLocation> explicitTargets) {
        this.colorGetter = color;
        this.lifeGetter = life;
        this.sizeGetter = size;
        this.redGetter = red;
        this.greenGetter = green;
        this.blueGetter = blue;
        this.alphaGetter = alpha;
        this.speedGetter = speed;
        this.explicitTargets = explicitTargets;
        this.filter = filter;
        this.colormap = colormap;
    }

    public static ParticleModifier ofColor(String color) {
        ParticleContextExpression expression = ParticleContextExpression.parse(color);
        return new ParticleModifier(null, null, expression, null, null, null, null,
                null, null, null, Set.of());
    }


    public void modify(Particle particle, Level level, ParticleOptions options) {
        if (filter != null) {
            if (!filter.test(options)) return;
        }
        if (colorGetter != null) {
            float[] unpack = ColorUtils.unpack((int) colorGetter.getValue(particle, level));
            particle.setColor(unpack[0], unpack[1], unpack[2]);
        }
        if (colormap != null) {
            BlockState state = null;
            if (options instanceof BlockParticleOption bo) {
                state = bo.getState();
            }
            float[] unpack = ColorUtils.unpack(colormap.getColor(state, level, BlockPos.containing(particle.x, particle.y, particle.z), 0));
            particle.setColor(unpack[0], unpack[1], unpack[2]);
        }
        if (lifeGetter != null) {
            particle.setLifetime((int) lifeGetter.getValue(particle, level));
        }
        if (sizeGetter != null) {
            particle.scale((float) sizeGetter.getValue(particle, level));
        }
        if (redGetter != null) {
            particle.rCol = (float) redGetter.getValue(particle, level);
        }
        if (greenGetter != null) {
            particle.gCol = (float) greenGetter.getValue(particle, level);
        }
        if (blueGetter != null) {
            particle.bCol = (float) blueGetter.getValue(particle, level);
        }
        if (speedGetter != null) {
            double speed = speedGetter.getValue(particle, level);
            particle.xd *= speed;
            particle.yd *= speed;
            particle.zd *= speed;
        }
        if (alphaGetter != null) {
            particle.alpha = (float) alphaGetter.getValue(particle, level);
        }
    }

    @Override
    public @NotNull Set<ResourceLocation> explicitTargets() {
        return explicitTargets;
    }

    private record Filter(@Nullable Block forBlock,
                          @Nullable Item forItem) implements Predicate<ParticleOptions> {

        Filter(Optional<Block> state, Optional<Item> item) {
            this(state.orElse(null), item.orElse(null));
        }

        public static final Codec<Filter> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
                BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("block").forGetter(p -> Optional.ofNullable(p.forBlock)),
                BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("item").forGetter(p -> Optional.ofNullable(p.forItem))
        ).apply(instance, Filter::new));

        @Override
        public boolean test(ParticleOptions particleOptions) {
            if (forBlock != null && particleOptions instanceof BlockParticleOption bo) {
                return bo.getState().getBlock() == forBlock;
            }
            if (forItem != null && particleOptions instanceof ItemParticleOption io) {
                return io.getItem().getItem() == forItem;
            }
            return true;
        }
    }
}
