package net.mehvahdjukaar.polytone.common.expressions.impl;

import hollowpoint.nexp.api.ExpProgram;
import net.mehvahdjukaar.polytone.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.common.expressions.proxies.ParticleProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.RandomProxy;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.Level;

public class ParticleExp extends PolyExp implements IParticleExp {

    public static final PolyExpType<ParticleExp> TYPE = new PolyExpType<>(ParticleExp::new,
            c -> c.input(ParticleProxy.class, "o", "object").input(RandomProxy.class, "r", "random"));

    protected ParticleExp(ExpProgram program, String source) {
        super(program, source);
    }

    @Override
    public double evaluate(Particle particle, Level level) {
        return executeDouble(new ParticleProxy(particle, level), RandomProxy.GLOBAL);
    }
}
