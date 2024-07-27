package net.mehvahdjukaar.polytone.neoforge;

import com.github.alexmodguy.alexscaves.client.ClientProxy;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class AlexsCavesCompat {

    public static float modifyGamma(float partialTicks, float gamma) {
        float biomeAmbientLight = ClientProxy.lastBiomeAmbientLightAmountPrev + (ClientProxy.lastBiomeAmbientLightAmount - ClientProxy.lastBiomeAmbientLightAmountPrev) * partialTicks;
        if(biomeAmbientLight > 0.0F){
            gamma = Mth.clamp(gamma + biomeAmbientLight, 0.0F, 1.0F);
        }
        return gamma;
    }

    public static void applyACLightingColors(ClientLevel level, Vector3f combined, float partialTicks) {
        if (!level.effects().forceBrightLightmap()) {
            Vec3 in = new Vec3(combined);
            Vec3 to = ClientProxy.lastBiomeLightColorPrev.add(ClientProxy.lastBiomeLightColor.subtract(ClientProxy.lastBiomeLightColorPrev).scale(partialTicks));
            combined.set(to.x * in.x, to.y * in.y, to.z * in.z);
        }
    }
}
