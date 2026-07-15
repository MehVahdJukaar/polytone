package net.mehvahdjukaar.polytone.content.common.expressions.impl;

import net.mehvahdjukaar.polytone.content.common.expressions.ExpUtils;
import net.mehvahdjukaar.polytone.content.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.content.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.content.common.expressions.proxies.ParticleProxy;
import net.mehvahdjukaar.polytone.content.common.expressions.proxies.RandomProxy;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.Level;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ParticleExp extends PolyExp implements IParticleExp {

    public static final PolyExpType<ParticleExp> TYPE =
            new PolyExpType<>(
                    ParticleExp::new,
                    c -> {
                        ExpUtils.addCommonInputs(c);
                        c.addInput("o", ParticleProxy.class);
                        c.addInput("object", ParticleProxy.class);
                    }
            );

    protected ParticleExp(Serializable expr) {
        super(expr);
    }

    @Override
    public double evaluate(Particle particle, Level level) {
        ParticleProxy obj = new ParticleProxy(particle, level);
        Map<String, Object> vars = new HashMap<>();
        ExpUtils.addCommonVars(vars);
        vars.put("o", obj);
        vars.put("object", obj);
        RandomProxy rand = RandomProxy.GLOBAL;
        vars.put("random", rand);
        vars.put("r", rand);
        return executeDouble(vars);
    }


}
