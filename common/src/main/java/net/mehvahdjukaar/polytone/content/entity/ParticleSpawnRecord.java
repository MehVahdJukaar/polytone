package net.mehvahdjukaar.polytone.content.entity;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public record ParticleSpawnRecord(Vec3 pos, EntityParticleEmitter emitter) {
}
