package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.minecraft.world.inventory.Slot;

import java.util.Optional;

public record SlotModifier(Optional<IntRange> targets, int xOffset, int yOffset, int zOffset,
                           Optional<IntRange> targetX, Optional<IntRange> targetY, Optional<String> targetClass) {

    public static final SchemaCodec<SlotModifier> CODEC = SchemaRecord.create(SlotModifier.class, i -> i.group(
            i.optional("slots", IntRange.CODEC, SlotModifier::targets),
            i.optional("x_offset", Codec.INT, 0, SlotModifier::xOffset),
            i.optional("y_offset", Codec.INT, 0, SlotModifier::yOffset),
            i.optional("z_offset", Codec.INT, 0, SlotModifier::zOffset),
            i.optional("target_x", IntRange.CODEC, SlotModifier::targetX),
            i.optional("target_y", IntRange.CODEC, SlotModifier::targetY),
            i.optional("target_class_name", Codec.STRING.xmap(PlatStuff::maybeRemapName, PlatStuff::maybeRemapName), SlotModifier::targetClass)
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
