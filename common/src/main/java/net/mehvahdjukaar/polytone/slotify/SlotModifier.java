package net.mehvahdjukaar.polytone.slotify;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.minecraft.world.inventory.Slot;

import java.util.Optional;

public record SlotModifier(Optional<IntRange> targets, int xOffset, int yOffset, int zOffset,
                           Optional<IntRange> targetX, Optional<IntRange> targetY, Optional<String> targetClass) {

    public static final Codec<SlotModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            IntRange.CODEC.optionalFieldOf("slots").forGetter(SlotModifier::targets),
            Codec.INT.optionalFieldOf("x_offset", 0).forGetter(SlotModifier::xOffset),
            Codec.INT.optionalFieldOf("y_offset", 0).forGetter(SlotModifier::yOffset),
            Codec.INT.optionalFieldOf("z_offset", 0).forGetter(SlotModifier::zOffset),
            IntRange.CODEC.optionalFieldOf("target_x").forGetter(SlotModifier::targetX),
            IntRange.CODEC.optionalFieldOf("target_y").forGetter(SlotModifier::targetY),
            Codec.STRING.xmap(PlatStuff::maybeRemapName, PlatStuff::maybeRemapName).optionalFieldOf("target_class_name").forGetter(SlotModifier::targetClass)
    ).apply(i, SlotModifier::new));

    public void modify(Slot slot) {

        slot.x += this.xOffset;
        slot.y += this.yOffset;
    }


    public boolean matches(Slot slot) {
        if (targets.isPresent() && !targets.get().has(slot.index)) return false;
        if (targetX.isPresent() && targetX.get().has(slot.x)) return false;
        if (targetY.isPresent() && targetY.get().has(slot.y)) return false;
        if (targetClass.isPresent()) {
            String name = targetClass.get();
            if (!slot.getClass().getSimpleName().equals(name) &&
                    !slot.getClass().getName().equals(name)) return false;
        }
        return true;
    }

    public boolean hasOffset() {
        return xOffset != 0 || yOffset != 0;
    }

}
