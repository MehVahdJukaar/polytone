package net.mehvahdjukaar.polytone.mixins.accessor;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Particle.class)
public interface ParticleAccessor {

    @Invoker("setSize")
    void invokeSetSize(float a, float b);

}
