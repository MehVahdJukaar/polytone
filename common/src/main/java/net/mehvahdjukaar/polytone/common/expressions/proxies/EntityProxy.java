package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class EntityProxy extends AbstractEntityProxy{

    private final Entity entity;
    private final @Nullable LivingEntity le;

    public EntityProxy(Entity entity) {
        this.entity = entity;
        if (entity instanceof LivingEntity l) {
            this.le = l;
        } else {
            this.le = null;
        }
    }

    @Override
    public Entity entity() {
        return  entity;
    }

    @Override
    protected @Nullable LivingEntity livingEntity() {
        return le;
    }
}
