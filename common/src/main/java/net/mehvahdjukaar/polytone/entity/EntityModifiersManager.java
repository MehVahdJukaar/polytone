package net.mehvahdjukaar.polytone.entity;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.misc.reloader.JsonPartialReloader;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Map;

public class EntityModifiersManager extends JsonPartialReloader {


    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> obj, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {

    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {

    }

    @Override
    protected void resetWithLevel(boolean logOff) {

    }

    public void onEntityTick(Entity entity){

    }
}
