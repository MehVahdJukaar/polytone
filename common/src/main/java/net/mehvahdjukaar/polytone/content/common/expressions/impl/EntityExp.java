package net.mehvahdjukaar.polytone.content.common.expressions.impl;

import hollowpoint.nexp.api.ExpProgram;
import net.mehvahdjukaar.polytone.content.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.content.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.content.common.expressions.proxies.EntityProxy;
import net.mehvahdjukaar.polytone.content.common.expressions.proxies.RandomProxy;
import net.minecraft.world.entity.Entity;

public class EntityExp extends PolyExp implements IEntityExp {

    public static final PolyExpType<EntityExp> TYPE = new PolyExpType<>(EntityExp::new,
            c -> c.input(EntityProxy.class, "o", "object").input(RandomProxy.class, "r", "random"));

    protected EntityExp(ExpProgram program, String source) {
        super(program, source);
    }

    @Override
    public double evaluate(Entity entity) {
        return executeDouble(new EntityProxy(entity), RandomProxy.GLOBAL);
    }
}
