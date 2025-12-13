package net.mehvahdjukaar.polytone.utils;

import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public interface IExtendedBlockElementRotation {

    Vector3f getRotation();

    void setRotation(Vector3f axis);



}
