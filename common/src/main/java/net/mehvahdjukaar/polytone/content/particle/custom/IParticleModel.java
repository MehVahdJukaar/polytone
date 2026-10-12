package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;

public interface IParticleModel {

    Codec<IParticleModel> CODEC = SchemaCodecs.labeled(
            SchemaCodecs.alternatives(BakedParticleModel.CODEC, EntityParticleModel.CODEC),
            SchemaCodecs.alt("baked model", Identifier.CODEC),
            SchemaCodecs.alt("entity model", EntityParticleModel.CODEC));

    void submit(CustomParticleInstance particle, SubmitNodeCollector collector, PoseStack poseStack, int light, int color, float partialTicks);

    default void tick(CustomParticleInstance particle, ClientLevel level) {
    }
}
