package net.mehvahdjukaar.polytone.mixins.neoforge;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ParticleEngine.class)
public interface ParticleEngineAccessor {

    @Accessor("providers")
    Map<Identifier, ParticleProvider<?>> getProviders();

}
