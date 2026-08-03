package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

@BeanAliases
public abstract class AbstractEntityProxy extends PositionalProxy {

    protected abstract Entity entity();

    @Nullable
    protected abstract LivingEntity livingEntity();

    @Override
    protected Level getLevelInternal() {
        return entity().level();
    }

    @Override
    protected BlockPos getPosInternal() {
        return entity().blockPosition();
    }

    public String name() {
        return entity().getName().getString();
    }

    public int age() {
        return entity().tickCount;
    }

    public double x() {
        return entity().getX();
    }

    public double y() {
        return entity().getY();
    }

    public double z() {
        return entity().getZ();
    }

    public double xd() {
        return entity().getDeltaMovement().x;
    }

    public double yd() {
        return entity().getDeltaMovement().y;
    }

    public double zd() {
        return entity().getDeltaMovement().z;
    }

    public boolean inWater() {
        return entity().isInWater();
    }

    public boolean onFire() {
        return entity().isOnFire();
    }

    public double hurtTime() {
        LivingEntity le = livingEntity();
        if (le == null) return 0;
        return le.hurtTime;
    }

    public String mainHandItem() {
        LivingEntity le = livingEntity();
        if (le == null) return "minecraft:air";
        return le.getMainHandItem().getItemHolder().getRegisteredName();
    }

    public String offHandItem() {
        LivingEntity le = livingEntity();
        if (le == null) return "minecraft:air";
        return le.getOffhandItem().getItemHolder().getRegisteredName();
    }

    public String armor(String slot) {
        LivingEntity le = livingEntity();
        if (le == null) return "minecraft:air";
        EquipmentSlot eq = switch (slot) {
            case "feet" -> EquipmentSlot.FEET;
            case "legs" -> EquipmentSlot.LEGS;
            case "chest" -> EquipmentSlot.CHEST;
            case "head" -> EquipmentSlot.HEAD;
            default -> EquipmentSlot.MAINHAND;
        };
        if (eq == EquipmentSlot.MAINHAND) return "air";
        return le.getItemBySlot(eq).getItemHolder().getRegisteredName();
    }

    public double height() {
        return entity().getBbHeight();
    }

    public double width() {
        return entity().getBbWidth();
    }

    public double eyeHeight() {
        return entity().getEyeHeight();
    }

    public double health() {
        LivingEntity le = livingEntity();
        return le == null ? 1 : le.getHealth();
    }

    public double maxHealth() {
        LivingEntity le = livingEntity();
        return le == null ? 1 : le.getMaxHealth();
    }

    public double walkAnimation() {
        LivingEntity le = livingEntity();
        return le == null ? 0 : le.walkAnimation.position();
    }

    public double walkAnimationSpeed() {
        LivingEntity le = livingEntity();
        return le == null ? 0 : le.walkAnimation.speed();
    }

    public boolean swimming() {
        return entity().isSwimming();
    }

    public boolean flying() {
        LivingEntity le = livingEntity();
        return le != null && le.isFallFlying();
    }

    public boolean crouching() {
        LivingEntity le = livingEntity();
        return entity().isCrouching();
    }

    public boolean sleeping() {
        LivingEntity le = livingEntity();
        return le != null && le.isSleeping();
    }

    public boolean onGround() {
        return entity().onGround();
    }

    public double speed() {
        return entity().getDeltaMovement().length();
    }

    public double speedSq() {
        return entity().getDeltaMovement().lengthSqr();
    }


}
