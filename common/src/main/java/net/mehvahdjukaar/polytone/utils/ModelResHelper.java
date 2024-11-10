package net.mehvahdjukaar.polytone.utils;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

public class ModelResHelper {

    public static final Codec<ModelResourceLocation> MODEL_RES_CODEC = ResourceLocation.CODEC.xmap(ModelResHelper::of,
            ModelResourceLocation::id);

    public static ModelResourceLocation of(ResourceLocation id){
        return new ModelResourceLocation(id, !Polytone.isForge ? "fabric_resource" : "standalone");
    }
}
