package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import org.jetbrains.annotations.Nullable;

@BeanAliases
public class EntityProxy extends AbstractEntityProxy {

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

    @Nullable
    public EntityProxy owner() {
        if (entity instanceof TraceableEntity t) {
            var owner = t.getOwner();
            if (owner != null) {
                return new EntityProxy(owner);
            }
        }
        return null;
    }

    public String entityType() {
        Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key == null ? "[unregistered]" : key.toString();
    }

    // Villager/zombie-villager profession id (e.g. minecraft:cleric), or "" if not a villager.
    public String profession() {
        if (entity instanceof Villager v) {
            return v.getVillagerData().profession()
                    .unwrapKey().map(k -> k.identifier().toString()).orElse("");
        }
        return "";
    }

    @Override
    protected Entity entity() {
        return entity;
    }

    @Override
    protected @Nullable LivingEntity livingEntity() {
        return le;
    }
}
