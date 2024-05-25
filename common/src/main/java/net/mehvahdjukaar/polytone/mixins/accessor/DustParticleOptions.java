package net.mehvahdjukaar.polytone.mixins.accessor;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.core.particles.DustParticleOptions.class)
public interface DustParticleOptions {

    @Mutable
    @Accessor("color")
    void setColor(Vector3f color);
}
