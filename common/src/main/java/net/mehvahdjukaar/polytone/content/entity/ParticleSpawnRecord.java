package net.mehvahdjukaar.polytone.content.entity;

import org.joml.Matrix4fc;

public record ParticleSpawnRecord(Matrix4fc matrix, EntityParticleEmitter emitter) {
}
