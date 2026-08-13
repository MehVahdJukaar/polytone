package net.mehvahdjukaar.polytone.content.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

// Codec backed entity model: mirrors what a hardcoded create*Layer() method would build, so it can
// become a real LayerDefinition. Rotations are authored in degrees.
public record ModelDefinition(int[] textureSize, Map<String, Bone> bones) {

    static final Codec<Vector3f> VEC3 = ExtraCodecs.VECTOR3F.xmap(Vector3f::new, v -> v);

    static final Codec<int[]> IVEC2 = Codec.INT.listOf().comapFlatMap(
            l -> l.size() == 2 ? DataResult.success(new int[]{l.get(0), l.get(1)})
                    : DataResult.error(() -> "Expected 2 elements, got " + l.size()),
            a -> List.of(a[0], a[1]));

    public static final Codec<ModelDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            IVEC2.optionalFieldOf("texture_size", new int[]{64, 32}).forGetter(ModelDefinition::textureSize),
            Codec.unboundedMap(Codec.STRING, Bone.CODEC).fieldOf("bones").forGetter(ModelDefinition::bones)
    ).apply(i, ModelDefinition::new));

    public LayerDefinition toLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        bones.forEach((name, bone) -> bone.addTo(root, name));
        return LayerDefinition.create(mesh, textureSize[0], textureSize[1]);
    }

    public record Bone(Pose pose, List<Cube> cubes, Map<String, Bone> children) {

        public static final Codec<Bone> CODEC = Codec.recursive("PolytoneBone", self ->
                RecordCodecBuilder.create(i -> i.group(
                        Pose.CODEC.optionalFieldOf("pose", Pose.ZERO).forGetter(Bone::pose),
                        Cube.CODEC.listOf().optionalFieldOf("cubes", List.of()).forGetter(Bone::cubes),
                        Codec.unboundedMap(Codec.STRING, self).optionalFieldOf("children", Map.of()).forGetter(Bone::children)
                ).apply(i, Bone::new)));

        void addTo(PartDefinition parent, String name) {
            CubeListBuilder builder = CubeListBuilder.create();
            for (Cube c : cubes) {
                builder.texOffs(c.uv()[0], c.uv()[1]);
                builder.mirror(c.mirror());
                builder.addBox(c.origin().x, c.origin().y, c.origin().z,
                        c.size().x, c.size().y, c.size().z, new CubeDeformation(c.inflate()));
            }
            builder.mirror(false);
            PartDefinition self = parent.addOrReplaceChild(name, builder, pose.toPartPose());
            children.forEach((childName, child) -> child.addTo(self, childName));
        }
    }

    // origin is the corner (vanilla addBox origin), uv the texture offset
    public record Cube(Vector3f origin, Vector3f size, int[] uv, float inflate, boolean mirror) {

        public static final Codec<Cube> CODEC = RecordCodecBuilder.create(i -> i.group(
                VEC3.fieldOf("origin").forGetter(Cube::origin),
                VEC3.fieldOf("size").forGetter(Cube::size),
                IVEC2.optionalFieldOf("uv", new int[]{0, 0}).forGetter(Cube::uv),
                Codec.FLOAT.optionalFieldOf("inflate", 0f).forGetter(Cube::inflate),
                Codec.BOOL.optionalFieldOf("mirror", false).forGetter(Cube::mirror)
        ).apply(i, Cube::new));
    }

    // rotation is authored in degrees, converted to radians at bake
    public record Pose(Vector3f offset, Vector3f rotation) {

        public static final Pose ZERO = new Pose(new Vector3f(), new Vector3f());

        public static final Codec<Pose> CODEC = RecordCodecBuilder.create(i -> i.group(
                VEC3.optionalFieldOf("offset", new Vector3f()).forGetter(Pose::offset),
                VEC3.optionalFieldOf("rotation", new Vector3f()).forGetter(Pose::rotation)
        ).apply(i, Pose::new));

        public PartPose toPartPose() {
            return PartPose.offsetAndRotation(offset.x, offset.y, offset.z,
                    (float) Math.toRadians(rotation.x),
                    (float) Math.toRadians(rotation.y),
                    (float) Math.toRadians(rotation.z));
        }
    }
}
