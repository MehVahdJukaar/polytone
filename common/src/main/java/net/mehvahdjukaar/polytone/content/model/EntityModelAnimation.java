package net.mehvahdjukaar.polytone.content.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.ModelPartUtils;
import net.mehvahdjukaar.polytone.common.struc.Vec3T;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToDoubleFunction;

public class EntityModelAnimation<E> {

    private final Map<String, Bone<E>> bones;
    private final List<String> names = new ArrayList<>();
    private final List<Bone<E>> ordered = new ArrayList<>();

    public EntityModelAnimation(Map<String, Bone<E>> bones) {
        this.bones = bones;
        bones.forEach((name, bone) -> {
            names.add(name);
            ordered.add(bone);
        });
    }

    public static <E> Codec<EntityModelAnimation<E>> codec(Codec<E> expression) {
        Codec<Map<String, Bone<E>>> byBone = Codec.unboundedMap(Codec.STRING, Bone.codec(expression));
        return SchemaCodecs.labeled(byBone.xmap(EntityModelAnimation::new, a -> a.bones),
                SchemaCodecs.alt("bones", byBone));
    }

    //every tick
    public Pose tick(@Nullable Pose pose, ToDoubleFunction<E> evaluator) {
        boolean first = pose == null;
        if (first) pose = new Pose(ordered.size());
        else pose.advance();
        for (int i = 0; i < ordered.size(); i++) {
            Bone<E> bone = ordered.get(i);
            if (bone.rotation.isPresent()) {
                Vector3f r = bone.rotation.get().evaluate(evaluator);
                pose.setRotation(i, r.x, r.y, r.z);
            }
            if (bone.offset.isPresent()) {
                Vector3f o = bone.offset.get().evaluate(evaluator);
                pose.setOffset(i, o.x, o.y, o.z);
            }
            pose.setScale(i, bone.scale.map(e -> (float) evaluator.applyAsDouble(e)).orElse(1F));
            pose.setVisible(i, bone.visible.map(e -> evaluator.applyAsDouble(e) > 0).orElse(true));
        }
        if (first) pose.advance();
        return pose;
    }

    // same order as the poses. null where the model has no such bone
    public ModelPart[] findBones(ModelPart root) {
        ModelPart[] parts = new ModelPart[names.size()];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = ModelPartUtils.findBone(root, names.get(i));
            if (parts[i] == null) {
                Polytone.LOGGER.warn("Model animation targets bone '{}' but the model has no bone with that name", names.get(i));
            }
        }
        return parts;
    }

    //every render frame, applies the thing
    public void apply(ModelPart[] parts, @Nullable Pose pose, float partialTicks) {
        for (int i = 0; i < parts.length; i++) {
            ModelPart part = parts[i];
            if (part == null) continue;
            part.resetPose();
            if (pose == null) {
                part.visible = true;
                continue;
            }
            part.offsetRotation(pose.getRotation(i, partialTicks));
            part.offsetPos(pose.getOffset(i, partialTicks));
            float scale = pose.getScale(i, partialTicks);
            part.xScale *= scale;
            part.yScale *= scale;
            part.zScale *= scale;
            part.visible = pose.isVisible(i);
        }
    }

    public record Bone<E>(Optional<Vec3T<E>> rotation, Optional<Vec3T<E>> offset, Optional<E> scale, Optional<E> visible) {

        public static <E> Codec<Bone<E>> codec(Codec<E> expression) {
            Codec<Vec3T<E>> xyz = Vec3T.codec(expression);
            return RecordCodecBuilder.create(i -> i.group(
                    xyz.optionalFieldOf("rotation").forGetter(Bone::rotation),
                    xyz.optionalFieldOf("offset").forGetter(Bone::offset),
                    expression.optionalFieldOf("scale").forGetter(Bone::scale),
                    expression.optionalFieldOf("visible").forGetter(Bone::visible)
            ).apply(i, Bone::new));
        }
    }


    //stosrs packed per bone pose info: rotation xyz, offset xyz, scale, visible
    public static class Pose {
        private static final int CHANNELS = 8;
        private static final int ROTATION = 0;
        private static final int OFFSET = 3;
        private static final int SCALE = 6;
        private static final int VISIBLE = 7;

        private final float[] current;
        private final float[] old;

        private Pose(int bones) {
            this.current = new float[bones * CHANNELS];
            this.old = new float[bones * CHANNELS];
        }

        private void advance() {
            System.arraycopy(current, 0, old, 0, current.length);
        }

        private void setRotation(int bone, float xDegrees, float yDegrees, float zDegrees) {
            int i = bone * CHANNELS + ROTATION;
            current[i] = xDegrees;
            current[i + 1] = yDegrees;
            current[i + 2] = zDegrees;
        }

        private void setOffset(int bone, float x, float y, float z) {
            int i = bone * CHANNELS + OFFSET;
            current[i] = x;
            current[i + 1] = y;
            current[i + 2] = z;
        }

        private void setScale(int bone, float scale) {
            current[bone * CHANNELS + SCALE] = scale;
        }

        private void setVisible(int bone, boolean visible) {
            current[bone * CHANNELS + VISIBLE] = visible ? 1 : 0;
        }

        //radians
        public Vector3f getRotation(int bone, float partialTicks) {
            int i = bone * CHANNELS + ROTATION;
            return new Vector3f(
                    Mth.rotLerp(partialTicks, old[i], current[i]),
                    Mth.rotLerp(partialTicks, old[i + 1], current[i + 1]),
                    Mth.rotLerp(partialTicks, old[i + 2], current[i + 2])).mul(Mth.DEG_TO_RAD);
        }

        public Vector3f getOffset(int bone, float partialTicks) {
            int i = bone * CHANNELS + OFFSET;
            return new Vector3f(
                    Mth.lerp(partialTicks, old[i], current[i]),
                    Mth.lerp(partialTicks, old[i + 1], current[i + 1]),
                    Mth.lerp(partialTicks, old[i + 2], current[i + 2]));
        }

        public float getScale(int bone, float partialTicks) {
            int i = bone * CHANNELS + SCALE;
            return Mth.lerp(partialTicks, old[i], current[i]);
        }

        public boolean isVisible(int bone) {
            return current[bone * CHANNELS + VISIBLE] > 0;
        }
    }
}
