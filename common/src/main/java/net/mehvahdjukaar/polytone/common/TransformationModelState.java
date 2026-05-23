package net.mehvahdjukaar.polytone.common;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.core.BlockMath;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class TransformationModelState implements ModelState {
    private final Transformation transformation;
    private final boolean uvLock;

    private final Map<Direction, Matrix4fc> faceMapping = new EnumMap<>(Direction.class);
    private final Map<Direction, Matrix4fc> inverseFaceMapping = new EnumMap<>(Direction.class);

    public TransformationModelState(Transformation transformation, boolean uvLock) {
        this.transformation = transformation;
        this.uvLock = uvLock;

        if (uvLock) {
            for (Direction direction : Direction.values()) {
                Matrix4fc matrix4fc = BlockMath.getFaceTransformation(this.transformation, direction).getMatrix();
                this.faceMapping.put(direction, matrix4fc);
                this.inverseFaceMapping.put(direction, matrix4fc.invertAffine(new Matrix4f()));
            }
        }
    }

    @Override
    public @NotNull Transformation transformation() {
        return transformation;
    }

    @Override
    public Matrix4fc faceTransformation(Direction direction) {
        return this.faceMapping.getOrDefault(direction, NO_TRANSFORM);
    }

    @Override
    public Matrix4fc inverseFaceTransformation(Direction direction) {
        return this.inverseFaceMapping.getOrDefault(direction, NO_TRANSFORM);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TransformationModelState) obj;
        return Objects.equals(this.transformation, that.transformation) &&
                this.uvLock == that.uvLock;
    }

    @Override
    public int hashCode() {
        return Objects.hash(transformation, uvLock);
    }

    @Override
    public String toString() {
        return "TransformationModelState[" +
                "transformation=" + transformation + ", " +
                "uvLock=" + uvLock + ']';
    }

}
