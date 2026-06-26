package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@BeanAliases
public class PlayerProxy extends AbstractEntityProxy {

    public static final PlayerProxy INSTANCE = new PlayerProxy();

    public PlayerProxy() {
    }

    @Override
    protected Player entity() {
        return Minecraft.getInstance().player;
    }

    @Override
    protected LivingEntity livingEntity() {
        return Minecraft.getInstance().player;
    }

    public double itemUseTicks(){
        return entity().getTicksUsingItem();
    }

    public String itemUsed(){
        return entity().getUseItem().typeHolder().getRegisteredName();
    }
}
