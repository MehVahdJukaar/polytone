package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.mehvahdjukaar.polytone.utils.TargetsHelper;
import net.minecraft.client.particle.Particle;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public class ParticleModifier {

    public static final Codec<ParticleModifier> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            StrOpt.of(ParticleExpression.CODEC, "color").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            StrOpt.of(ParticleExpression.CODEC, "life").forGetter(p -> Optional.ofNullable(p.lifeGetter)),
            StrOpt.of(ParticleExpression.CODEC, "size").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            StrOpt.of(ParticleExpression.CODEC, "red").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            StrOpt.of(ParticleExpression.CODEC, "green").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            StrOpt.of(ParticleExpression.CODEC, "blue").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            StrOpt.of(ParticleExpression.CODEC, "alpha").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            StrOpt.of(ParticleExpression.CODEC, "speed").forGetter(p -> Optional.ofNullable(p.speedGetter)),
            StrOpt.of(TargetsHelper.CODEC, "targets").forGetter(p -> p.explicitTargets)

    ).apply(instance, ParticleModifier::new));


    @Nullable
    public ParticleExpression colorGetter;
    @Nullable
    public ParticleExpression lifeGetter;
    @Nullable
    public ParticleExpression sizeGetter;
    @Nullable
    public ParticleExpression speedGetter;
    @Nullable
    public ParticleExpression redGetter;
    @Nullable
    public ParticleExpression blueGetter;
    @Nullable
    public ParticleExpression greenGetter;
    @Nullable
    public ParticleExpression alphaGetter;
    public Optional<Set<ResourceLocation>> explicitTargets;

    private ParticleModifier(Optional<ParticleExpression> color, Optional<ParticleExpression> life,
                             Optional<ParticleExpression> size, Optional<ParticleExpression> red,
                             Optional<ParticleExpression> green, Optional<ParticleExpression> blue,
                             Optional<ParticleExpression> alpha, Optional<ParticleExpression> speed,
                             Optional<Set<ResourceLocation>> explicitTargets) {
        this(color.orElse(null), life.orElse(null), size.orElse(null),
                red.orElse(null), green.orElse(null), blue.orElse(null),
                alpha.orElse(null), speed.orElse(null), explicitTargets);
    }

    public ParticleModifier(@Nullable ParticleExpression color, @Nullable ParticleExpression life,
                            @Nullable ParticleExpression size, @Nullable ParticleExpression red,
                            @Nullable ParticleExpression green, @Nullable ParticleExpression blue,
                            @Nullable ParticleExpression alpha, @Nullable ParticleExpression speed,
                            Optional<Set<ResourceLocation>> explicitTargets) {
        this.colorGetter = color;
        this.lifeGetter = life;
        this.sizeGetter = size;
        this.redGetter = red;
        this.greenGetter = green;
        this.blueGetter = blue;
        this.alphaGetter = alpha;
        this.speedGetter = speed;
        this.explicitTargets = explicitTargets;
    }

    public static ParticleModifier ofColor(String color) {
        ParticleExpression expression = ParticleExpression.parse(color);
        return new ParticleModifier(expression, null, null, null, null,
                null, null, null, Optional.empty());
    }


    public void modify(Particle particle) {
        if (colorGetter != null) {
            float[] unpack = ColorUtils.unpack((int) colorGetter.get(particle));
            particle.setColor(unpack[0], unpack[1], unpack[2]);
        }
        if (lifeGetter != null) {
            particle.setLifetime((int) lifeGetter.get(particle));
        }
        if (sizeGetter != null) {
            particle.scale((float) sizeGetter.get(particle));
        }
        if (redGetter != null) {
            particle.rCol = (float) redGetter.get(particle);
        }
        if (greenGetter != null) {
            particle.gCol = (float) greenGetter.get(particle);
        }
        if (blueGetter != null) {
            particle.bCol = (float) blueGetter.get(particle);
        }
        if (speedGetter != null) {
            double speed = speedGetter.get(particle);
            particle.xd *= speed;
            particle.yd *= speed;
            particle.zd *= speed;
        }
        if (alphaGetter != null) {
            particle.alpha = (float) alphaGetter.get(particle);
        }
    }
}
