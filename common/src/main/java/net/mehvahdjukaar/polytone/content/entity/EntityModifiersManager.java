package net.mehvahdjukaar.polytone.content.entity;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Map;

public class EntityModifiersManager extends JsonPartialReloader {
    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> obj, RegistryOps<JsonElement> ops, RegistryAccess access) {

    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {

    }

    @Override
    protected void resetWithLevel(boolean logOff) {

    }

    public void onEntityTick(Entity entity){

    }
}
