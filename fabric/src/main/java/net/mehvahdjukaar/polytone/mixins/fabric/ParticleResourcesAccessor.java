package net.mehvahdjukaar.polytone.mixins.fabric;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.ParticleResources;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ParticleResources.class)
public interface ParticleResourcesAccessor {

    @Accessor("providers")
    Int2ObjectMap<ParticleProvider<?>> getProviders();

}
