package net.mehvahdjukaar.polytone.common;

import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ModelPartUtils {

    @Nullable
    public static ModelPart findBone(ModelPart root, String bone) {
        List<ModelPart> path = findBonePath(root, bone);
        return path == null ? null : path.getLast();
    }

    //full path of this bone
    @Nullable
    public static List<ModelPart> findBonePath(ModelPart root, String bone) {
        List<ModelPart> path = new ArrayList<>();
        path.add(root);
        if (bone.indexOf('.') == -1) {
            return addPathToName(root, bone, path) ? path : null;
        }
        ModelPart part = root;
        for (String name : bone.split("\\.")) {
            part = part.children.get(name);
            if (part == null) return null;
            path.add(part);
        }
        return path;
    }

    private static boolean addPathToName(ModelPart part, String name, List<ModelPart> path) {
        ModelPart child = part.children.get(name);
        if (child != null) {
            path.add(child);
            return true;
        }
        for (ModelPart c : part.children.values()) {
            path.add(c);
            if (addPathToName(c, name, path)) return true;
            path.removeLast();
        }
        return false;
    }
}
