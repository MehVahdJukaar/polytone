package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@BeanAliases
public class ParticleProxy extends PositionalProxy {
    private final ClientLevel level;
    private final Particle particle;
    private final BlockPos pos;
    @Nullable
    private final SingleQuadParticle quadParticle;

    public ParticleProxy(Particle particle, ClientLevel level) {
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
    protected ClientLevel getLevelInternal() {
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

    public int color() {
        if (quadParticle != null) {
            return ColorUtils.pack(quadParticle.rCol, quadParticle.gCol, quadParticle.bCol);
        }
        return -1;
    }

    public double custom() {
        if (particle instanceof CustomParticleInstance cp) {
            return cp.getCustom();
        }
        return 0;
    }

    /** Named custom variable, declared under the particle's {@code customs}. Names are case-insensitive. */
    public double custom(String name) {
        if (particle instanceof CustomParticleInstance cp) {
            return cp.getCustom(name);
        }
        return 0;
    }

    @Override
    public boolean hasEntitiesWithin() {
        return !level.getEntities(null, particle.getBoundingBox().inflate(1.25)).isEmpty();
    }


    public void xd(double xd) {
        particle.xd = xd;
    }

    public void yd(double yd) {
        particle.yd = yd;
    }

    public void zd(double zd) {
        particle.zd = zd;
    }

    public void red(double r) {
        if (quadParticle != null) quadParticle.rCol = (float) r;
    }


    public void green(double g) {
        if (quadParticle != null) quadParticle.gCol = (float) g;
    }

    public void blue(double b) {
        if (quadParticle != null) quadParticle.bCol = (float) b;
    }

    public void alpha(double a) {
        if (quadParticle != null) quadParticle.alpha = (float) a;
    }

    public void roll(double roll) {
        if (quadParticle != null) quadParticle.roll = (float) roll;
    }

    public void size(double size) {
        if (quadParticle != null) quadParticle.quadSize = (float) size;
    }

    public void custom(double custom) {
        if (particle instanceof CustomParticleInstance cp) {
            cp.setCustom(custom);
        }
    }

    public void custom(String name, double value) {
        if (particle instanceof CustomParticleInstance cp) {
            cp.setCustom(name, value);
        }
    }

    public void color(int rgb) {
        float[] unpack = ColorUtils.unpack(rgb);
        if (quadParticle != null) {
            quadParticle.setColor(unpack[0], unpack[1], unpack[2]);
        }
    }

    public void removed() {
        particle.remove();
    }

    public void colorFrom(String colormapName) {
        var c = Polytone.COLORMAPS.get(colormapName);
        if (c == null) {
            Polytone.LOGGER.error("Colormap with name '{}' not found!", colormapName);
            return;
        }
        BlockState state = Blocks.AIR.defaultBlockState();
        int color = c.colorInWorld(state, Minecraft.getInstance().level,
                BlockPos.containing(particle.x, particle.y, particle.z));

        this.color(color);
    }
}
