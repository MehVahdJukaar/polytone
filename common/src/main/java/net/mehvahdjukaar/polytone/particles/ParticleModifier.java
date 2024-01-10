package net.mehvahdjukaar.polytone.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.colors.ColorManager;
import net.minecraft.client.particle.Particle;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ParticleModifier {

    public static final Codec<ParticleModifier> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            ParticleExpression.CODEC.optionalFieldOf("color").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            ParticleExpression.CODEC.optionalFieldOf("life").forGetter(p -> Optional.ofNullable(p.lifeGetter)),
            ParticleExpression.CODEC.optionalFieldOf("size").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            ParticleExpression.CODEC.optionalFieldOf("red").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            ParticleExpression.CODEC.optionalFieldOf("green").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            ParticleExpression.CODEC.optionalFieldOf("blue").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            ParticleExpression.CODEC.optionalFieldOf("alpha").forGetter(p -> Optional.ofNullable(p.colorGetter)),
            ParticleExpression.CODEC.optionalFieldOf("speed").forGetter(p -> Optional.ofNullable(p.speedGetter))

    ).apply(instance, ParticleModifier::new));

    private ParticleModifier(Optional<ParticleExpression> color, Optional<ParticleExpression> life,
                             Optional<ParticleExpression> size, Optional<ParticleExpression> red,
                             Optional<ParticleExpression> green, Optional<ParticleExpression> blue,
                             Optional<ParticleExpression> alpha, Optional<ParticleExpression> speed) {
        this.colorGetter = color.orElse(null);
        this.lifeGetter = life.orElse(null);
        this.sizeGetter = size.orElse(null);
        this.speedGetter = speed.orElse(null);
        this.redGetter = red.orElse(null);
        this.greenGetter = green.orElse(null);
        this.blueGetter = blue.orElse(null);
        this.alphaGetter = alpha.orElse(null);
    }

    public ParticleModifier(@Nullable ParticleExpression color,@Nullable ParticleExpression life,
                           @Nullable ParticleExpression size, @Nullable ParticleExpression red,
                           @Nullable ParticleExpression green,@Nullable ParticleExpression blue,
                           @Nullable ParticleExpression alpha,@Nullable ParticleExpression speed){
        this.colorGetter = color;
        this.lifeGetter = life;
        this.sizeGetter = size;
        this.redGetter = red;
        this.greenGetter = green;
        this.blueGetter = blue;
        this.alphaGetter = alpha;
        this.speedGetter = speed;
    }

    public static ParticleModifier ofColor(String color){
        ParticleExpression expression = ParticleExpression.parse(color);
        return new ParticleModifier(expression,null, null, null,null,null,null,null);
    }


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

    public void modify(Particle particle) {
        if (colorGetter != null) {
            float[] unpack = ColorManager.unpack((int) colorGetter.get(particle));
            particle.setColor(unpack[0], unpack[1], unpack[2]);
        }
        if(lifeGetter != null){
            particle.setLifetime((int) lifeGetter.get(particle));
        }
        if(sizeGetter != null){
            particle.scale((float) sizeGetter.get(particle));
        }
        if(redGetter != null){
            particle.rCol = (float) redGetter.get(particle);
        }
        if(greenGetter != null){
            particle.gCol = (float) greenGetter.get(particle);
        }
        if(blueGetter != null){
            particle.bCol = (float) blueGetter.get(particle);
        }
        if(speedGetter != null){
            double speed = speedGetter.get(particle);
            particle.xd *= speed;
            particle.yd *= speed;
            particle.zd *= speed;
        }
        if(alphaGetter != null){
            particle.alpha = alphaGetter.get(particle);
        }
    }
}
