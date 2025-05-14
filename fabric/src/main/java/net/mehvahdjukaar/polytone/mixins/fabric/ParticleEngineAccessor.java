package net.mehvahdjukaar.polytone.mixins.fabric;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(ParticleEngine.class)
public interface ParticleEngineAccessor {

    @Accessor("providers")
    Int2ObjectMap<ParticleProvider<?>> getProviders();

    @Accessor("RENDER_ORDER")
    @Mutable
    static void setRENDER_ORDER(List<ParticleRenderType> value) {
        throw new UnsupportedOperationException();
    }

    @Accessor("RENDER_ORDER")
    @Final
    static List<ParticleRenderType> getRENDER_ORDER() {
        throw new UnsupportedOperationException();
    }
}
