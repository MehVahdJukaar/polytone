package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.npc.Villager;
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
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key == null ? "[unregistered]" : key.toString();
    }

    /** Villager/zombie-villager profession id (e.g. {@code minecraft:cleric}), or "" if not a villager. */
    public String profession() {
        if (entity instanceof Villager v) {
            ResourceLocation key = BuiltInRegistries.VILLAGER_PROFESSION.getKey(v.getVillagerData().getProfession());
            return key == null ? "" : key.toString();
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
