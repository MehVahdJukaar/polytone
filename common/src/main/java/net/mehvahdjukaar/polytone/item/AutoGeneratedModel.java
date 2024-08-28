package net.mehvahdjukaar.polytone.item;

import net.minecraft.resources.ResourceLocation;

public record AutoGeneratedModel(ResourceLocation modelLocation, ResourceLocation texturePath) {

    public static AutoGeneratedModel ofCIM(ResourceLocation cimPath) {
        ResourceLocation modelLocation = cimPath.withPrefix("polytone_generated_");
        return new AutoGeneratedModel(modelLocation, cimPath.withPrefix("polytone/custom_item_models/"));
    }
}