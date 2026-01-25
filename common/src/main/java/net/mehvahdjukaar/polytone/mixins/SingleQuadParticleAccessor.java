package net.mehvahdjukaar.polytone.mixins;

import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SingleQuadParticle.class)
public interface SingleQuadParticleAccessor {

    @Invoker("getLayer")
    SingleQuadParticle.Layer invokeGetLayer();
}
