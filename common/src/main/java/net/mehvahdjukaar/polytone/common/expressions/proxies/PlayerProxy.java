package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.mehvahdjukaar.polytone.common.expressions.ExpTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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

    // Hot values read ExpTicker's per-tick player cache (refreshed each tick and again at
    // particle-batch dispatch) so async workers never touch the live entity. Between ticks
    // the player doesn't move, so cached and live reads agree; null cache -> live fallback.

    @Override
    public double x() {
        var s = ExpTicker.playerSnapshot();
        return s != null ? s.x() : super.x();
    }

    @Override
    public double y() {
        var s = ExpTicker.playerSnapshot();
        return s != null ? s.y() : super.y();
    }

    @Override
    public double z() {
        var s = ExpTicker.playerSnapshot();
        return s != null ? s.z() : super.z();
    }

    @Override
    public double xd() {
        var s = ExpTicker.playerSnapshot();
        return s != null ? s.xd() : super.xd();
    }

    @Override
    public double yd() {
        var s = ExpTicker.playerSnapshot();
        return s != null ? s.yd() : super.yd();
    }

    @Override
    public double zd() {
        var s = ExpTicker.playerSnapshot();
        return s != null ? s.zd() : super.zd();
    }

    @Override
    public double speed() {
        var s = ExpTicker.playerSnapshot();
        return s != null ? s.speed() : super.speed();
    }

    @Override
    public double width() {
        var s = ExpTicker.playerSnapshot();
        return s != null ? s.width() : super.width();
    }

    @Override
    public double height() {
        var s = ExpTicker.playerSnapshot();
        return s != null ? s.height() : super.height();
    }

    @Override
    public boolean crouching() {
        var s = ExpTicker.playerSnapshot();
        return s != null ? s.crouching() : super.crouching();
    }

    public double itemUseTicks() {
        return entity().getTicksUsingItem();
    }

    public String itemUsed() {
        ItemStack stack = entity().getUseItem();
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? "minecraft:air" : key.toString();
    }
}
