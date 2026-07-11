package net.mehvahdjukaar.polytone.content.particle.modifiers;

import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.Targets;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.exp.impl.ParticleContextExpression;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

public class ParticleModifier {


    //TODO: make single expression
    public static final SchemaCodec<ParticleModifier> CODEC = SchemaRecord.create(ParticleModifier.class, i -> i.group(
            i.optional("filter", Filter.CODEC, p -> Optional.ofNullable(p.filter)),
            i.optional("colormap", Colormap.CODEC, p -> Optional.ofNullable(p.colormap)),
            i.optional("color", ParticleContextExpression.CODEC, p -> Optional.ofNullable(p.colorGetter)),
            i.optional("life", ParticleContextExpression.CODEC, p -> Optional.ofNullable(p.lifeGetter)),
            i.optional("size", ParticleContextExpression.CODEC, p -> Optional.ofNullable(p.colorGetter)),
            i.optional("red", ParticleContextExpression.CODEC, p -> Optional.ofNullable(p.colorGetter)),
            i.optional("green", ParticleContextExpression.CODEC, p -> Optional.ofNullable(p.colorGetter)),
            i.optional("blue", ParticleContextExpression.CODEC, p -> Optional.ofNullable(p.colorGetter)),
            i.optional("alpha", ParticleContextExpression.CODEC, p -> Optional.ofNullable(p.colorGetter)),
            i.optional("speed", ParticleContextExpression.CODEC, p -> Optional.ofNullable(p.speedGetter)),
            i.optional("targets", Targets.CODEC, Targets.EMPTY, p -> p.targets)
    ).apply(i, ParticleModifier::new));

    public static final SchemaCodec<ParticleModifier> PARTIAL_CODEC = SchemaRecord.create(ParticleModifier.class, i ->
            i.group(
                    i.optional("colormap", Colormap.CODEC, p -> Optional.ofNullable(p.colormap))
            ).apply(i, c -> ParticleModifier.ofColormap(c.orElse(null))));


    @Nullable
    private final Filter filter;
    @Nullable
    public IColorGetter colormap;
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
    public final Targets targets;

    private ParticleModifier(Optional<Filter> filter, Optional<IColorGetter> colormap,
                             Optional<ParticleContextExpression> color, Optional<ParticleContextExpression> life,
                             Optional<ParticleContextExpression> size, Optional<ParticleContextExpression> red,
                             Optional<ParticleContextExpression> green, Optional<ParticleContextExpression> blue,
                             Optional<ParticleContextExpression> alpha, Optional<ParticleContextExpression> speed,
                             Targets targets) {
        this(filter.orElse(null), colormap.orElse(null), color.orElse(null), life.orElse(null), size.orElse(null),
                red.orElse(null), green.orElse(null), blue.orElse(null),
                alpha.orElse(null), speed.orElse(null), targets);
    }

    private ParticleModifier(@Nullable Filter filter, @Nullable IColorGetter colormap,
                            @Nullable ParticleContextExpression color, @Nullable ParticleContextExpression life,
                            @Nullable ParticleContextExpression size, @Nullable ParticleContextExpression red,
                            @Nullable ParticleContextExpression green, @Nullable ParticleContextExpression blue,
                            @Nullable ParticleContextExpression alpha, @Nullable ParticleContextExpression speed,
                            Targets explicitTargets) {
        this.colorGetter = color;
        this.lifeGetter = life;
        this.sizeGetter = size;
        this.redGetter = red;
        this.greenGetter = green;
        this.blueGetter = blue;
        this.alphaGetter = alpha;
        this.speedGetter = speed;
        this.targets = explicitTargets;
        this.filter = filter;
        this.colormap = colormap;
    }

    public static ParticleModifier ofColor(String color) {
        ParticleContextExpression expression = new ParticleContextExpression(color);
        return new ParticleModifier(null, null, expression, null, null, null, null,
                null, null, null, Targets.EMPTY);
    }

    public static ParticleModifier ofColormap(IColorGetter colormap) {
        return new ParticleModifier(null, colormap, null, null, null, null, null,
                null, null, null, Targets.EMPTY);
    }


    public Targets targets() {
        return this.targets;
    }

    public void modify(@NotNull SingleQuadParticle particle, Level level, ParticleOptions options) {
        if (filter != null) {
            if (!filter.test(options)) return;
        }
        if (colorGetter != null) {
            float[] unpack = ColorUtils.unpack((int) colorGetter.evaluate(particle, level));
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
            particle.setLifetime((int) lifeGetter.evaluate(particle, level));
        }
        if (sizeGetter != null) {
            particle.scale((float) sizeGetter.evaluate(particle, level));
        }
        if (redGetter != null) {
            particle.rCol = (float) redGetter.evaluate(particle, level);
        }
        if (greenGetter != null) {
            particle.gCol = (float) greenGetter.evaluate(particle, level);
        }
        if (blueGetter != null) {
            particle.bCol = (float) blueGetter.evaluate(particle, level);
        }
        if (speedGetter != null) {
            double speed = speedGetter.evaluate(particle, level);
            particle.xd *= speed;
            particle.yd *= speed;
            particle.zd *= speed;
        }
        if (alphaGetter != null) {
            particle.alpha = (float) alphaGetter.evaluate(particle, level);
        }
    }

    public boolean hasColormap() {
        return this.colormap != null;
    }

    public void setColormap(IColorGetter colormap) {
        this.colormap = colormap;
    }

    public BlockColor getColormap() {
        return this.colormap;
    }


    private record Filter(@Nullable Block forBlock,
                          @Nullable Item forItem) implements Predicate<ParticleOptions> {

        Filter(Optional<Optional<Block>> state, Optional<Optional<Item>> item) {
            this(state.flatMap(x -> x).orElse(null), item.flatMap(x -> x).orElse(null));
        }

        public static final SchemaCodec<Filter> CODEC = SchemaRecord.create(Filter.class, i -> i.group(
                i.optional("block", CodecUtils.forwardAwareByNameCodec(BuiltInRegistries.BLOCK), p -> Optional.of(Optional.ofNullable(p.forBlock))),
                i.optional("item", CodecUtils.forwardAwareByNameCodec(BuiltInRegistries.ITEM), p -> Optional.of(Optional.ofNullable(p.forItem)))
        ).apply(i, Filter::new));

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
