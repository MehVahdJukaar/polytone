package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.content.slotify.SlotifySlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Slot.class)
public abstract class SlotMixin implements SlotifySlot {

    @Shadow
    public int x;
    @Shadow
    public int y;

    @Unique
    private boolean polytone$baseCaptured = false;
    @Unique
    private int polytone$baseX;
    @Unique
    private int polytone$baseY;

    @Override
    public void polytone$captureBase() {
        if (!polytone$baseCaptured) {
            polytone$baseX = this.x;
            polytone$baseY = this.y;
            polytone$baseCaptured = true;
        }
    }

    @Override
    public void polytone$resetToBase() {
        if (polytone$baseCaptured) {
            this.x = polytone$baseX;
            this.y = polytone$baseY;
        }
    }
}
