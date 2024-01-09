package net.mehvahdjukaar.polytone.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ParticleModifier {

    public static final Codec<ParticleModifier> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            ParticleExpression.CODEC.optionalFieldOf("color" ).forGetter(p -> Optional.ofNullable( p.colorGetter)),
            ParticleExpression.CODEC.optionalFieldOf("size" ).forGetter(p -> Optional.ofNullable(p.colorGetter)),
            ParticleExpression.CODEC.optionalFieldOf("red").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            ParticleExpression.CODEC.optionalFieldOf("green" ).forGetter(p -> Optional.ofNullable(p.colorGetter)),
            ParticleExpression.CODEC.optionalFieldOf("blue").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            ParticleExpression.CODEC.optionalFieldOf("alpha" ).forGetter(p ->Optional.ofNullable( p.colorGetter)),
            ParticleExpression.CODEC.optionalFieldOf("speed" ).forGetter(p -> Optional.ofNullable(p.speedGetter))

    ).apply(instance, ParticleModifier::new));

    private ParticleModifier(Optional<ParticleExpression> color, Optional<ParticleExpression> size, Optional<ParticleExpression> red,
                            Optional<ParticleExpression> green, Optional<ParticleExpression> blue,
                            Optional<ParticleExpression> alpha, Optional<ParticleExpression> speed) {
        this.colorGetter = color.orElse(null);
        this.sizeGetter = size.orElse(null);
        this.speedGetter = speed.orElse(null);
        this.redGetter = red.orElse(null);
        this.greenGetter = green.orElse(null);
        this.blueGetter = blue.orElse(null);
        this.alphaGetter = alpha.orElse(null);
    }


    @Nullable
    public ParticleExpression colorGetter;
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
}
