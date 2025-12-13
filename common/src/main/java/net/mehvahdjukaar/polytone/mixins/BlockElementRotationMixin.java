package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.utils.IExtendedBlockElementRotation;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockElementRotation.class)
public class BlockElementRotationMixin implements IExtendedBlockElementRotation {

    @Unique
    @Nullable
    private Vector3f polytone$rotation = null;

    @Nullable
    @Override
    public Vector3f getRotation() {
        return this.polytone$rotation;
    }

    @Override
    public void setRotation(Vector3f rot) {
        this.polytone$rotation = rot;
    }
}
