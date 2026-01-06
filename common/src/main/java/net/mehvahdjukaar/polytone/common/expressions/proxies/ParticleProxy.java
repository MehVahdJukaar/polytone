package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanGettersAliases;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

@BeanGettersAliases
public class ParticleProxy extends PositionalProxy {
    private final Level level;
    private final Particle particle;
    private final BlockPos pos;
    @Nullable
    private final SingleQuadParticle quadParticle;

    public ParticleProxy(Particle particle, Level level) {
        super();
        this.particle = particle;
        if (particle instanceof SingleQuadParticle sqp) {
            this.quadParticle = sqp;
        } else {
            this.quadParticle = null;
        }
        this.level = level;
        this.pos = BlockPos.containing(particle.x, particle.y, particle.z);
    }

    @Override
    protected Level getLevelInternal() {
        return level;
    }

    @Override
    protected BlockPos getPosInternal() {
        return pos;
    }

    public double x() {
        return particle.x;
    }

    public double y() {
        return particle.y;
    }

    public double z() {
        return particle.z;
    }

    public double xd() {
        return particle.xd;
    }

    public double yd() {
        return particle.yd;
    }

    public double zd() {
        return particle.zd;
    }

    public double red() {
        return quadParticle != null ? quadParticle.rCol : 1.0;
    }

    public double green() {
        return quadParticle != null ? quadParticle.gCol : 1.0;
    }

    public double blue() {
        return quadParticle != null ? quadParticle.bCol : 1.0;
    }

    public double alpha() {
        return quadParticle != null ? quadParticle.alpha : 1.0;
    }

    public double roll() {
        return quadParticle != null ? quadParticle.roll : 0.0;
    }

    public double size() {
        return quadParticle != null ? quadParticle.getQuadSize(0f) : 0.15F;
    }

    public int age() {
        return particle.age;
    }

    public int life() {
        return particle.getLifetime();
    }

    public boolean hasEntitiesWithin() {
        return !level.getEntities(null, particle.getBoundingBox().inflate(1.25)).isEmpty();
    }

}
