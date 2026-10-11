package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;

public interface IParticleModel {

    Codec<IParticleModel> CODEC = SchemaCodecs.labeled(
            SchemaCodecs.alternatives(BakedParticleModel.CODEC, EntityParticleModel.CODEC),
            SchemaCodecs.alt("baked model", ResourceLocation.CODEC),
            SchemaCodecs.alt("entity model", EntityParticleModel.CODEC));

    void render(CustomParticleInstance particle, PoseStack poseStack, int light, float r, float g, float b, float a, float partialTicks);

    default void tick(CustomParticleInstance particle, ClientLevel level) {
    }
}
