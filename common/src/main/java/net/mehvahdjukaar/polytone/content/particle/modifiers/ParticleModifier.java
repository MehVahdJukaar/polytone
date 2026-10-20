package net.mehvahdjukaar.polytone.content.particle.modifiers;

import net.mehvahdjukaar.polytone.common.exp.impl.ParticleContextExpression;
import net.mehvahdjukaar.polytone.common.expressions.impl.IParticleExp;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.content.shaders.light.ColoredLight;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.Targets;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ParticleModifier {

    public static final Codec<ParticleModifier> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            Filter.CODEC.optionalFieldOf("filter").forGetter(p -> Optional.ofNullable(p.filter)),
            Colormap.CODEC.optionalFieldOf("colormap").forGetter(p -> Optional.ofNullable(p.colormap)),
            IParticleExp.CODEC_LEGACY.optionalFieldOf("color").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            IParticleExp.CODEC_LEGACY.optionalFieldOf("life").forGetter(p -> Optional.ofNullable(p.lifeGetter)),
            IParticleExp.CODEC_LEGACY.optionalFieldOf("size").forGetter(p -> Optional.ofNullable(p.sizeGetter)),
            IParticleExp.CODEC_LEGACY.optionalFieldOf("red").forGetter(p -> Optional.ofNullable(p.redGetter)),
            IParticleExp.CODEC_LEGACY.optionalFieldOf("green").forGetter(p -> Optional.ofNullable(p.greenGetter)),
            IParticleExp.CODEC_LEGACY.optionalFieldOf("blue").forGetter(p -> Optional.ofNullable(p.blueGetter)),
            IParticleExp.CODEC_LEGACY.optionalFieldOf("alpha").forGetter(p -> Optional.ofNullable(p.alphaGetter)),
            IParticleExp.CODEC_LEGACY.optionalFieldOf("speed").forGetter(p -> Optional.ofNullable(p.speedGetter)),
            ColoredLight.ParticleLight.CODEC
                    .optionalFieldOf("colored_light").forGetter(p -> Optional.ofNullable(p.coloredLight)),
            Targets.codec(Registries.PARTICLE_TYPE).optionalFieldOf("targets", Targets.EMPTY).forGetter(p -> p.targets)

    ).apply(instance, ParticleModifier::new));

    public static final Codec<ParticleModifier> PARTIAL_CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(
                    Colormap.CODEC.optionalFieldOf( "colormap").forGetter(p -> Optional.ofNullable(p.colormap))
            ).apply(instance, c -> ParticleModifier.ofColormap(c.orElse(null))));


    @Nullable
    private final Filter filter;
    @Nullable
    public IColorGetter colormap;
    @Nullable
    public final IParticleExp colorGetter;
    @Nullable
    public final IParticleExp lifeGetter;
    @Nullable
    public final IParticleExp sizeGetter;
    @Nullable
    public final IParticleExp speedGetter;
    @Nullable
    public final IParticleExp redGetter;
    @Nullable
    public final IParticleExp blueGetter;
    @Nullable
    public final IParticleExp greenGetter;
    @Nullable
    public final IParticleExp alphaGetter;
    @Nullable
    public final ColoredLight.ParticleLight coloredLight;
    public final Targets targets;

    private ParticleModifier(Optional<Filter> filter, Optional<IColorGetter> colormap,
                             Optional<IParticleExp> color, Optional<IParticleExp> life,
                             Optional<IParticleExp> size, Optional<IParticleExp> red,
                             Optional<IParticleExp> green, Optional<IParticleExp> blue,
                             Optional<IParticleExp> alpha, Optional<IParticleExp> speed,
                             Optional<ColoredLight.ParticleLight> coloredLight,
                             Targets targets) {
        this(filter.orElse(null), colormap.orElse(null), color.orElse(null), life.orElse(null), size.orElse(null),
                red.orElse(null), green.orElse(null), blue.orElse(null),
                alpha.orElse(null), speed.orElse(null), coloredLight.orElse(null), targets);
    }

    public ParticleModifier(@Nullable Filter filter, @Nullable IColorGetter colormap,
                            @Nullable IParticleExp color, @Nullable IParticleExp life,
                            @Nullable IParticleExp size, @Nullable IParticleExp red,
                            @Nullable IParticleExp green, @Nullable IParticleExp blue,
                            @Nullable IParticleExp alpha, @Nullable IParticleExp speed,
                            @Nullable ColoredLight.ParticleLight coloredLight,
                            Targets explicitTargets) {
        this.colorGetter = color;
        this.lifeGetter = life;
        this.sizeGetter = size;
        this.redGetter = red;
        this.greenGetter = green;
        this.blueGetter = blue;
        this.alphaGetter = alpha;
        this.speedGetter = speed;
        this.coloredLight = coloredLight;
        this.targets = explicitTargets;
        this.filter = filter;
        this.colormap = colormap;
    }

    public static ParticleModifier ofColor(String color) {
        ParticleContextExpression expression = new ParticleContextExpression(color);
        return new ParticleModifier(null, null, expression::getValue, null, null, null, null,
                null, null, null, null, Targets.EMPTY);
    }

    public static ParticleModifier ofColormap(IColorGetter colormap) {
        return new ParticleModifier(null, colormap, null, null, null, null, null,
                null, null, null, null, Targets.EMPTY);
    }


    public Targets targets() {
        return this.targets;
    }

    public void modify(@NotNull Particle particle, Level level, ParticleOptions options) {
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
            //not getColor. that one goes through the block tint cache
            BlockPos pos = BlockPos.containing(particle.x, particle.y, particle.z);
            float[] unpack = ColorUtils.unpack(colormap.sampleColor(level, state, pos, level.getBiome(pos).value(), null));
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


    private record Filter(Set<Block> forBlocks,
                          Set<Item> forItems) implements Predicate<ParticleOptions> {

        public static final Codec<Filter> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
                setOf(BuiltInRegistries.BLOCK).optionalFieldOf("block", Set.of()).forGetter(Filter::forBlocks),
                setOf(BuiltInRegistries.ITEM).optionalFieldOf("item", Set.of()).forGetter(Filter::forItems)
        ).apply(instance, Filter::new));

        private static <E> Codec<Set<E>> setOf(Registry<E> registry) {
            return SchemaCodecs.singleOrList(CodecUtils.forwardAwareByNameCodec(registry))
                    .xmap(list -> list.stream().flatMap(Optional::stream).collect(Collectors.toUnmodifiableSet()),
                            set -> set.stream().map(Optional::of).toList());
        }

        @Override
        public boolean test(ParticleOptions particleOptions) {
            if (!forBlocks.isEmpty() && particleOptions instanceof BlockParticleOption bo) {
                return forBlocks.contains(bo.getState().getBlock());
            }
            if (!forItems.isEmpty() && particleOptions instanceof ItemParticleOption io) {
                return forItems.contains(io.getItem().getItem());
            }
            return true;
        }
    }
}
