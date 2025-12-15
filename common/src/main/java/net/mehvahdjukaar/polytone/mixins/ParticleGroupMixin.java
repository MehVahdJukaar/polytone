package net.mehvahdjukaar.polytone.mixins;

import com.google.common.collect.EvictingQueue;
import net.minecraft.client.particle.ParticleGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ParticleGroup.class)
public class ParticleGroupMixin {

    @ModifyArg(method = "<init>",
            require = 0, //low priority
            at = @At(value = "INVOKE", target = "Lcom/google/common/collect/EvictingQueue;create(I)Lcom/google/common/collect/EvictingQueue;"))
    private int polytone$increaseEvictingQueueSize(int size) {
        return size * 50;
    }
}
