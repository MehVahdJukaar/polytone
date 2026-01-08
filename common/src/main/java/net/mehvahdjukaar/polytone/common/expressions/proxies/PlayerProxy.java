package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanGettersAliases;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@BeanGettersAliases
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

}
