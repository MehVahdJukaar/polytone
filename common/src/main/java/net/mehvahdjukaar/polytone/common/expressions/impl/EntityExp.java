package net.mehvahdjukaar.polytone.common.expressions.impl;

import net.mehvahdjukaar.polytone.common.expressions.ExpUtils;
import net.mehvahdjukaar.polytone.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.common.expressions.proxies.AbstractEntityProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.EntityProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.ParticleProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.RandomProxy;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class EntityExp extends PolyExp implements IEntityExp {

    public static final PolyExpType<EntityExp> TYPE =
            new PolyExpType<>(
                    EntityExp::new,
                    c -> {
                        ExpUtils.addCommonInputs(c);
                        c.addInput("o", EntityProxy.class);
                        c.addInput("object", EntityProxy.class);
                    }
            );

    protected EntityExp(Serializable expr) {
        super(expr);
    }

    @Override
    public double evaluate(Entity entity) {
        EntityProxy obj = new EntityProxy(entity);
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
