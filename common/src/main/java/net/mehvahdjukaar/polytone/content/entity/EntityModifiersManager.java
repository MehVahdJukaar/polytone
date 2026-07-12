package net.mehvahdjukaar.polytone.content.entity;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.utils.ContentManager;
import net.mehvahdjukaar.polytone.utils.Parsed;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public class EntityModifiersManager extends ContentManager<EntityModifier, Map<ResourceLocation, JsonElement>> {

    private final Map<EntityType<?>, EntityModifier> emittersPerEntity = new HashMap<>();

    public EntityModifiersManager() {
        super("entity_modifier", () -> SchemaCodecs.labeled(EntityModifier.CODEC), "entity_modifiers");
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager) {
        return this.getJsonsInDirectories(resourceManager);
    }

    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops, RegistryAccess access) {
        for (var j : Parsed.batchParseOnlyEnabled(jsons, EntityModifier.CODEC, ops, "Entity Modifiers")) {
            if (j.getValue() != null) {
                addModifier(j.getKey(), j.getValue());
            }
        }
    }

    private void addModifier(ResourceLocation fileId, EntityModifier mod) {
        for (var h : mod.targets().compute(fileId, BuiltInRegistries.ENTITY_TYPE.asLookup())) {
            emittersPerEntity.merge(h.value(), mod, EntityModifier::merge);
        }
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        emittersPerEntity.clear();
    }

    public void onEntityTick(Entity entity) {
        if (emittersPerEntity.isEmpty()) return;
        EntityModifier mod = emittersPerEntity.get(entity.getType());
        if (mod != null) {
            mod.tick(entity);
        }
    }
}
