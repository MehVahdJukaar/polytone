package net.mehvahdjukaar.polytone.compat.platform;

public class AlexsCavesCompat {
//TODO: add back
/*
    public static float modifyGamma(float partialTicks, float gamma) {
        float biomeAmbientLight = ClientProxy.lastBiomeAmbientLightAmountPrev + (ClientProxy.lastBiomeAmbientLightAmount - ClientProxy.lastBiomeAmbientLightAmountPrev) * partialTicks;
        if(biomeAmbientLight > 0.0F){
            gamma = Mth.clamp(gamma + biomeAmbientLight, 0.0F, 1.0F);
        }
        return gamma;
    }

    public static void applyACLightingColors(ClientLevel level, Vector3f combined, float partialTicks) {
        if (!level.effects().hasEndFlashes()) {
            Vec3 in = new Vec3(combined);
            Vec3 to = ClientProxy.lastBiomeLightColorPrev.add(ClientProxy.lastBiomeLightColor.subtract(ClientProxy.lastBiomeLightColorPrev).scale(partialTicks));
            combined.set(to.x * in.x, to.y * in.y, to.z * in.z);
        }
    }*/
}
