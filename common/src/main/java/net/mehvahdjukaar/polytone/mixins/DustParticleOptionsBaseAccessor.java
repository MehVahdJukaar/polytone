package net.mehvahdjukaar.polytone.mixins;

import net.minecraft.core.particles.DustParticleOptionsBase;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DustParticleOptionsBase.class)
public interface DustParticleOptionsBaseAccessor {

    @Mutable
    @Accessor("color")
    void setColor(Vector3f color);
}
