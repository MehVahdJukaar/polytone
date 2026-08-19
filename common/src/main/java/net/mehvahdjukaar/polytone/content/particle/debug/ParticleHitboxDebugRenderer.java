package net.mehvahdjukaar.polytone.content.particle.debug;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.mehvahdjukaar.polytone.content.particle.custom.ParticleRenderMode;
import net.mehvahdjukaar.polytone.mixins.SingleQuadParticleAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public class ParticleHitboxDebugRenderer {

    public static final Identifier ID = Polytone.res("particle_hitboxes");

    public static void emitGizmos() {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.debugEntries.isCurrentlyEnabled(ID)) return;

        Vec3 camera = mc.gameRenderer.getMainCamera().position();

        for (var entry : mc.particleEngine.particles.entrySet()) {
            ParticleRenderType renderType = entry.getKey();
            ParticleGroup<?> group = entry.getValue();

            for (var p : group.getAll()) {
                if (camera.distanceToSqr(p.x, p.y, p.z) < 16 * 16) {

                    int color = getColor(renderType, p);

                    if (renderType == ParticleRenderType.NO_RENDER) {
                        // Solid translucent box
                        Gizmos.cuboid(p.getBoundingBox(), GizmoStyle.fill(color));
                    } else {
                        // Normal wireframe
                        Gizmos.cuboid(p.getBoundingBox(), GizmoStyle.stroke(color));
                    }
                }
            }
        }
    }

    private static int getColor(ParticleRenderType type, Particle p) {

        if (type == ParticleRenderType.SINGLE_QUADS) {
            if (p instanceof SingleQuadParticle sq) {
                SingleQuadParticle.Layer layer =
                        ((SingleQuadParticleAccessor) sq).invokeGetLayer();

                if (layer == SingleQuadParticle.Layer.TERRAIN) {
                    return 0xFF1CFF20; // green
                }
                else if (layer == SingleQuadParticle.Layer.ITEMS) {
                    return 0xFF9C27B0; // orange
                }
                else if (layer == SingleQuadParticle.Layer.OPAQUE) {
                    return 0xFF2196F3; // blue
                }
                else if (layer == ParticleRenderMode.CUSTOM_LAYER) {
                    return 0xFF9C27B0; //  light purple
                }
                else if (layer == SingleQuadParticle.Layer.TRANSLUCENT) {
                    return 0xFFFF9800; // orange
                }
                else if (layer == ParticleRenderMode.ADDITIVE_TRANSLUCENT_LAYER) {
                    return 0xFFFFFF00; // yellow
                }
            }

            // SINGLE_QUADS but layer unknown
            return 0xFFFF0000; //red
        }

        else if (type == ParticleRenderType.ITEM_PICKUP) {
            return 0xFFFF6091; // yellow
        }

        else if (type == ParticleRenderType.NO_RENDER) {
            return 0x66ff8888; // translucent indigo (used with fill)
        }

        else if (type == ParticleRenderType.ELDER_GUARDIANS) {
            return 0xFF00BCD4; // cyan
        }

        else if (type == PolytoneRenderTypes.PARTICLE_MODEL_GROUP) {
            return 0xFFFF00FF; // dark purple green
        }

        // Unknown render type → black
        return 0xFF000000;
    }
}


