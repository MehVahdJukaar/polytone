package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanGettersAliases;
import net.minecraft.world.entity.player.Player;

@BeanGettersAliases
public class PlayerProxy extends PositionalProxy {

    private final Player player;

    public PlayerProxy(Player player) {
        super(player.level(), player.getOnPos());
        this.player = player;

    }
}
