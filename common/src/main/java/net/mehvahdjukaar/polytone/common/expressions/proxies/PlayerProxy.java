package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanGettersAliases;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@BeanGettersAliases
public class PlayerProxy extends PositionalProxy {

    public static final PlayerProxy INSTANCE = new PlayerProxy();

    public PlayerProxy() {
    }

    @Override
    protected Level getLevelInternal() {
        return delegate().level();
    }

    @Override
    protected BlockPos getPosInternal() {
        return delegate().getOnPos();
    }

    private Player delegate() {
        return Minecraft.getInstance().player;
    }

    public int age() {
        return delegate().tickCount;
    }

    public double hurtTime() {
        return delegate().hurtTime;
    }

    public double x() {
        return delegate().getX();
    }

    public double y() {
        return delegate().getY();
    }

    public double z() {
        return delegate().getZ();
    }

    public double xd() {
        return delegate().getDeltaMovement().x;
    }

    public double yd() {
        return delegate().getDeltaMovement().y;
    }

    public double zd() {
        return delegate().getDeltaMovement().z;
    }

    public boolean inWater() {
        return delegate().isInWater();
    }

    public String mainHandItem() {
        return delegate().getMainHandItem().getItemHolder().getRegisteredName();
    }

    public String offHandItem() {
        return delegate().getOffhandItem().getItemHolder().getRegisteredName();
    }

    public String armor(String slot) {
        EquipmentSlot eq = switch (slot) {
            case "feet" -> EquipmentSlot.FEET;
            case "legs" -> EquipmentSlot.LEGS;
            case "chest" -> EquipmentSlot.CHEST;
            case "head" -> EquipmentSlot.HEAD;
            default -> EquipmentSlot.MAINHAND;
        };
        if (eq == EquipmentSlot.MAINHAND) return "air";
        return delegate().getItemBySlot(eq).getItemHolder().getRegisteredName();
    }

    public double health(){
        return delegate().getHealth();
    }

    public double maxHealth(){
        return delegate().getMaxHealth();
    }

    public double walkAnimation() {
        return delegate().walkAnimation.position();
    }

    public double walkAnimationSpeed() {
        return delegate().walkAnimation.speed();
    }

    public boolean swimming() {
        return delegate().isSwimming();
    }

    public boolean flying() {
        return delegate().isFallFlying();
    }

    public boolean crouching() {
        return delegate().isCrouching();
    }

    public boolean sleeping() {
        return delegate().isSleeping();
    }

    public boolean onGround(){
        return delegate().onGround();
    }

    public double speed(){
        return delegate().getSpeed();
    }


}
