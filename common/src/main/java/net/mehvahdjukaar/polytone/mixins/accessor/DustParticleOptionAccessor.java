package net.mehvahdjukaar.polytone.mixins.accessor;

import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DustParticleOptions.class)
public interface DustParticleOptionAccessor {

    @Mutable
    @Accessor("color")
    void setColor(Vector3f color);
}
